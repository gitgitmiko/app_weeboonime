package com.webunime.mobile.data.user

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.webunime.mobile.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private val Context.userStore by preferencesDataStore("webunime_user_economy")

/**
 * Ekonomi (kunci/gem/XP/level) terikat ke Firebase UID.
 * Ganti akun Google → load cloud akun itu (atau bootstrap baru), bukan data lokal akun lama.
 */
class UserRepository(private val context: Context) {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    private val kKeys = intPreferencesKey("keys")
    private val kGems = intPreferencesKey("gems")
    private val kXp = intPreferencesKey("xp")
    private val kLevel = intPreferencesKey("level")
    private val kPremiumUntil = longPreferencesKey("premium_until")
    private val kBootstrapped = intPreferencesKey("bootstrapped")
    private val kBoundUid = stringPreferencesKey("bound_uid")

    private val localFlow: Flow<UserProfile> = context.userStore.data.map { prefs ->
        val boot = prefs[kBootstrapped] ?: 0
        val keys = if (boot == 0) BuildConfig.STARTING_KEYS else (prefs[kKeys] ?: 0)
        UserProfile(
            keys = keys,
            gems = prefs[kGems] ?: 0,
            xp = prefs[kXp] ?: 0,
            level = (prefs[kLevel] ?: 1).coerceAtLeast(1),
            isPremium = (prefs[kPremiumUntil] ?: 0L) > System.currentTimeMillis(),
            premiumUntilMs = prefs[kPremiumUntil] ?: 0L,
        )
    }

    val profileFlow: Flow<UserProfile> = combine(localFlow, authStateFlow()) { local, authUser ->
        local.copy(
            uid = authUser?.uid,
            displayName = authUser?.displayName,
            email = authUser?.email,
            photoUrl = authUser?.photoUrl?.toString(),
        )
    }.distinctUntilChanged()

    private fun authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
        val a = auth
        if (a == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        a.addAuthStateListener(listener)
        trySend(a.currentUser)
        awaitClose { a.removeAuthStateListener(listener) }
    }

    suspend fun ensureBootstrapped() {
        context.userStore.edit { prefs ->
            if ((prefs[kBootstrapped] ?: 0) == 0) {
                writeFreshDefaults(prefs)
            }
        }
    }

    /**
     * Panggil saat login / ganti akun / logout.
     * - uid null → reset lokal (guest)
     * - uid beda dari bound → reset lokal lalu load cloud akun baru (tanpa merge akun lama)
     * - uid sama → sync cloud
     */
    suspend fun bindToAccount(uid: String?) {
        val bound = context.userStore.data.first()[kBoundUid]
        when {
            uid.isNullOrBlank() -> {
                context.userStore.edit { prefs ->
                    writeFreshDefaults(prefs)
                    prefs.remove(kBoundUid)
                }
            }
            bound != uid -> {
                context.userStore.edit { prefs ->
                    writeFreshDefaults(prefs)
                    prefs[kBoundUid] = uid
                }
                loadCloudForCurrentUser(overwriteLocal = true)
            }
            else -> {
                loadCloudForCurrentUser(overwriteLocal = false)
            }
        }
    }

    suspend fun current(): UserProfile = profileFlow.first()

    suspend fun consumeKeyForEpisode(slug: String, episode: Int): Result<UserProfile> {
        val now = System.currentTimeMillis()
        val before = current()
        if (before.effectivePremium(now)) {
            return Result.success(before)
        }
        if (before.keys <= 0) {
            return Result.failure(IllegalStateException("Kunci habis"))
        }
        val after = before.copy(keys = before.keys - 1)
        persistLocal(after)
        syncEconomyToCloud(after)
        val uid = auth?.currentUser?.uid
        val firestore = db
        if (uid != null && firestore != null) {
            runCatching {
                firestore.collection("users").document(uid)
                    .collection("unlocks")
                    .document("$slug#$episode")
                    .set(
                        mapOf(
                            "slug" to slug,
                            "episode" to episode,
                            "at" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    .await()
            }
        }
        return Result.success(after)
    }

    suspend fun grantKeys(amount: Int): UserProfile {
        val before = current()
        val after = before.copy(keys = before.keys + amount.coerceAtLeast(0))
        persistLocal(after)
        syncEconomyToCloud(after)
        return after
    }

    suspend fun exchangeGemsForKey(): Result<UserProfile> {
        val need = BuildConfig.GEMS_PER_KEY
        val before = current()
        if (before.gems < need) {
            return Result.failure(IllegalStateException("Gem kurang (butuh $need)"))
        }
        val after = before.copy(gems = before.gems - need, keys = before.keys + 1)
        persistLocal(after)
        syncEconomyToCloud(after)
        return Result.success(after)
    }

    suspend fun grantEpisodeXp(): UserProfile {
        val before = current()
        val after = EconomyRules.applyXp(
            before,
            BuildConfig.XP_PER_EPISODE,
            BuildConfig.GEMS_PER_LEVEL,
        )
        persistLocal(after)
        syncEconomyToCloud(after)
        return after
    }

    suspend fun applyPremiumDays(days: Int, bonusGems: Int): UserProfile {
        val now = System.currentTimeMillis()
        val before = current()
        val base = maxOf(before.premiumUntilMs, now)
        val until = base + days.toLong() * 24L * 60L * 60L * 1000L
        val after = before.copy(
            isPremium = true,
            premiumUntilMs = until,
            gems = before.gems + bonusGems.coerceAtLeast(0),
        )
        persistLocal(after)
        syncEconomyToCloud(after)
        return after
    }

    /** @deprecated gunakan [bindToAccount] */
    suspend fun pullCloudIfSignedIn() {
        val uid = auth?.currentUser?.uid ?: return
        bindToAccount(uid)
    }

    private suspend fun loadCloudForCurrentUser(overwriteLocal: Boolean) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val snap = withTimeoutOrNull(8_000L) {
            firestore.collection("users").document(uid).get().await()
        } ?: return // offline / timeout: jangan push defaults (bisa timpa cloud)
        if (!snap.exists()) {
            // Akun Google baru → mulai dari defaults (sudah di-reset), push ke cloud
            syncEconomyToCloud(current(), create = true)
            return
        }
        val cloud = UserProfile(
            uid = uid,
            displayName = auth?.currentUser?.displayName,
            email = auth?.currentUser?.email,
            photoUrl = auth?.currentUser?.photoUrl?.toString(),
            keys = (snap.getLong("keys") ?: BuildConfig.STARTING_KEYS.toLong()).toInt(),
            gems = (snap.getLong("gems") ?: 0L).toInt(),
            xp = (snap.getLong("xp") ?: 0L).toInt(),
            level = (snap.getLong("level") ?: 1L).toInt().coerceAtLeast(1),
            isPremium = snap.getBoolean("isPremium") == true,
            premiumUntilMs = snap.getLong("premiumUntilMs") ?: 0L,
        )
        if (overwriteLocal) {
            // Ganti akun: cloud akun ini saja, jangan max dengan lokal akun lama
            persistLocal(cloud)
        } else {
            // Sama akun: cloud menang untuk konsistensi multi-device
            persistLocal(cloud)
        }
    }

    private fun writeFreshDefaults(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[kKeys] = BuildConfig.STARTING_KEYS
        prefs[kGems] = 0
        prefs[kXp] = 0
        prefs[kLevel] = 1
        prefs[kPremiumUntil] = 0L
        prefs[kBootstrapped] = 1
    }

    private suspend fun persistLocal(profile: UserProfile) {
        context.userStore.edit { prefs ->
            prefs[kKeys] = profile.keys
            prefs[kGems] = profile.gems
            prefs[kXp] = profile.xp
            prefs[kLevel] = profile.level
            prefs[kPremiumUntil] = profile.premiumUntilMs
            prefs[kBootstrapped] = 1
            auth?.currentUser?.uid?.let { prefs[kBoundUid] = it }
        }
    }

    private suspend fun syncEconomyToCloud(profile: UserProfile, create: Boolean = false) {
        val uid = auth?.currentUser?.uid ?: return
        val ref = db?.collection("users")?.document(uid) ?: return
        val data = hashMapOf<String, Any>(
            "keys" to profile.keys,
            "gems" to profile.gems,
            "xp" to profile.xp,
            "level" to profile.level,
            "isPremium" to profile.effectivePremium(),
            "premiumUntilMs" to profile.premiumUntilMs,
            "email" to (auth?.currentUser?.email ?: ""),
            "displayName" to (auth?.currentUser?.displayName ?: ""),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (create) data["createdAt"] = FieldValue.serverTimestamp()
        runCatching { ref.set(data, SetOptions.merge()).await() }
    }
}
