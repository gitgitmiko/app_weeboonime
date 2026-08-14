package com.webunime.mobile.data.user

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import com.webunime.mobile.data.toEpisodeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private val Context.unlockStore by preferencesDataStore("webunime_episode_unlocks_v2")

class EpisodeUnlockStore(private val context: Context) {
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    private val kBoundUid = stringPreferencesKey("bound_uid")
    private val kUnlocks = stringSetPreferencesKey("unlocks")

    val unlocksFlow: Flow<Set<String>> = context.unlockStore.data.map { it[kUnlocks] ?: emptySet() }

    suspend fun bindToAccount(uid: String?) {
        val bound = context.unlockStore.data.first()[kBoundUid]
        when {
            uid.isNullOrBlank() -> {
                context.unlockStore.edit {
                    it[kUnlocks] = emptySet()
                    it.remove(kBoundUid)
                }
            }
            bound != uid -> {
                context.unlockStore.edit {
                    it[kUnlocks] = emptySet()
                    it[kBoundUid] = uid
                }
                pullCloud(uid)
            }
            else -> pullCloud(uid)
        }
    }

    suspend fun isUnlocked(slug: String, episode: Double): Boolean {
        val key = key(slug, episode)
        return (context.unlockStore.data.first()[kUnlocks] ?: emptySet()).contains(key)
    }

    suspend fun markUnlocked(slug: String, episode: Double) {
        mergeKeys(setOf(key(slug, episode)))
    }

    suspend fun mergeKeys(keys: Set<String>) {
        val clean = keys.map { it.trim() }.filter { it.contains('#') }.toSet()
        if (clean.isEmpty()) return
        context.unlockStore.edit { prefs ->
            val cur = prefs[kUnlocks]?.toMutableSet() ?: mutableSetOf()
            cur.addAll(clean)
            prefs[kUnlocks] = cur
        }
    }

    private suspend fun pullCloud(uid: String) {
        val firestore = db ?: return
        val snap = withTimeoutOrNull(8_000L) {
            firestore.collection("users").document(uid)
                .collection("unlocks")
                .limit(2_000)
                .get()
                .await()
        } ?: return
        val keys = snap.documents.mapNotNull { doc -> parseUnlockKey(doc.id, doc.data) }.toSet()
        if (keys.isEmpty()) return
        Log.i(TAG, "Pulled ${keys.size} episode unlocks from cloud")
        mergeKeys(keys)
    }

    companion object {
        private const val TAG = "EpisodeUnlocks"

        fun key(slug: String, episode: Double) =
            "$slug#${episode.toEpisodeKey()}"

        fun parseUnlockKey(docId: String, data: Map<String, Any>?): String? {
            val id = docId.trim()
            if (id.contains('#')) return id
            val slug = data?.get("slug")?.toString()?.trim().orEmpty()
            val episode = when (val raw = data?.get("episode")) {
                is Number -> raw.toDouble()
                is String -> raw.toDoubleOrNull()
                else -> null
            }
            if (slug.isEmpty() || episode == null || episode <= 0) return null
            return key(slug, episode)
        }
    }
}
