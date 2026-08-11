package com.webunime.mobile.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

data class WatchHistoryItem(
    val slug: String,
    val title: String,
    val thumbnail: String,
    val episode: Double?,
    val watchedAt: Long,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    fun progressFraction(): Float {
        if (durationMs <= 0L) return 0.08f
        return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * Riwayat menonton per akun Google (UID).
 * Lokal: SharedPreferences terpisah per uid. Cloud: field `watchHistory` di users/{uid}.
 */
class WatchHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    @Volatile
    private var activeUid: String? = null
    private var lastCloudPushAt = 0L

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    /**
     * Panggil saat login / ganti akun / logout.
     * Akun B tidak melihat riwayat akun A.
     */
    suspend fun bindToAccount(uid: String?) {
        if (!uid.isNullOrBlank()) {
            migrateLegacyIfNeeded(uid)
            absorbGuestInto(uid)
        }
        activeUid = uid?.takeIf { it.isNotBlank() }
        if (!activeUid.isNullOrBlank()) {
            pullCloudAndMerge(activeUid!!)
            pushCloud(force = true)
        }
        bump()
    }

    fun list(): List<WatchHistoryItem> = decode(prefs.getString(itemsKey(currentUid()), "[]"))

    /** Entri unik per anime (episode terakhir ditonton) — untuk home "Lanjutkan Menonton". */
    fun continueWatching(limit: Int = 12): List<WatchHistoryItem> {
        val seen = linkedSetOf<String>()
        return buildList {
            for (item in list()) {
                if (!seen.add(item.slug)) continue
                add(item)
                if (size >= limit) break
            }
        }
    }

    fun record(
        slug: String,
        title: String,
        thumbnail: String,
        episode: Double?,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
    ) {
        if (slug.isBlank()) return
        val now = System.currentTimeMillis()
        val current = list()
        val rest = current.filterNot { it.slug == slug && it.episode == episode }
        val prev = current.firstOrNull { it.slug == slug && it.episode == episode }
        val next = listOf(
            WatchHistoryItem(
                slug = slug,
                title = title.ifBlank { prev?.title ?: slug },
                thumbnail = thumbnail.ifBlank { prev?.thumbnail.orEmpty() },
                episode = episode,
                watchedAt = now,
                positionMs = if (positionMs > 0) positionMs else (prev?.positionMs ?: 0L),
                durationMs = if (durationMs > 0) durationMs else (prev?.durationMs ?: 0L),
            ),
        ) + rest
        save(next.take(MAX))
        pushCloud(force = false)
    }

    fun remove(slug: String, episode: Double?) {
        save(list().filterNot { it.slug == slug && it.episode == episode })
        pushCloud(force = true)
    }

    fun removeAll(keys: Set<Pair<String, Double?>>) {
        if (keys.isEmpty()) return
        save(list().filterNot { (it.slug to it.episode) in keys })
        pushCloud(force = true)
    }

    fun clear() {
        save(emptyList())
        pushCloud(force = true)
    }

    private fun currentUid(): String? = activeUid ?: auth?.currentUser?.uid

    private fun itemsKey(uid: String?): String =
        if (uid.isNullOrBlank()) KEY_GUEST else "items_$uid"

    private fun migrateLegacyIfNeeded(uid: String) {
        val legacy = prefs.getString(KEY_LEGACY, null)
        if (legacy.isNullOrBlank() || legacy == "[]") return
        val destKey = itemsKey(uid)
        val dest = prefs.getString(destKey, null)
        if (dest.isNullOrBlank() || dest == "[]") {
            prefs.edit().putString(destKey, legacy).remove(KEY_LEGACY).apply()
        } else {
            prefs.edit().remove(KEY_LEGACY).apply()
        }
    }

    private fun absorbGuestInto(uid: String) {
        val guest = decode(prefs.getString(KEY_GUEST, "[]"))
        if (guest.isEmpty()) return
        val destKey = itemsKey(uid)
        val dest = decode(prefs.getString(destKey, "[]"))
        saveToKey(destKey, mergeByLatest(dest, guest))
        prefs.edit().putString(KEY_GUEST, "[]").apply()
    }

    private suspend fun pullCloudAndMerge(uid: String) {
        val firestore = db ?: return
        val snap = withTimeoutOrNull(8_000L) {
            firestore.collection("users").document(uid).get().await()
        } ?: return
        @Suppress("UNCHECKED_CAST")
        val raw = snap.get("watchHistory") as? List<*> ?: return
        val cloud = raw.mapNotNull { row ->
            val m = row as? Map<*, *> ?: return@mapNotNull null
            val slug = m["slug"]?.toString()?.trim().orEmpty()
            if (slug.isEmpty()) return@mapNotNull null
            WatchHistoryItem(
                slug = slug,
                title = m["title"]?.toString().orEmpty(),
                thumbnail = m["thumbnail"]?.toString().orEmpty(),
                            episode = (m["episode"] as? Number)?.toDouble()?.takeIf { it > 0 },
                watchedAt = (m["watchedAt"] as? Number)?.toLong() ?: 0L,
                positionMs = (m["positionMs"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
                durationMs = (m["durationMs"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
            )
        }
        if (cloud.isEmpty()) return
        save(mergeByLatest(list(), cloud), notify = false)
    }

    private fun pushCloud(force: Boolean) {
        val uid = currentUid() ?: return
        val firestore = db ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastCloudPushAt < CLOUD_PUSH_MIN_MS) return
        lastCloudPushAt = now
        val payload = list().map { item ->
            hashMapOf<String, Any>(
                "slug" to item.slug,
                "title" to item.title,
                "thumbnail" to item.thumbnail,
                "episode" to (item.episode ?: -1),
                "watchedAt" to item.watchedAt,
                "positionMs" to item.positionMs,
                "durationMs" to item.durationMs,
            )
        }
        firestore.collection("users").document(uid)
            .set(
                hashMapOf(
                    "watchHistory" to payload,
                    "watchHistoryUpdatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
    }

    private fun mergeByLatest(
        a: List<WatchHistoryItem>,
        b: List<WatchHistoryItem>,
    ): List<WatchHistoryItem> {
        val map = linkedMapOf<Pair<String, Double?>, WatchHistoryItem>()
        for (item in a + b) {
            val key = item.slug to item.episode
            val prev = map[key]
            if (prev == null || item.watchedAt >= prev.watchedAt) map[key] = item
        }
        return map.values.sortedByDescending { it.watchedAt }.take(MAX)
    }

    private fun save(items: List<WatchHistoryItem>, notify: Boolean = true) {
        saveToKey(itemsKey(currentUid()), items)
        if (notify) bump()
    }

    private fun saveToKey(key: String, items: List<WatchHistoryItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("slug", item.slug)
                    .put("title", item.title)
                    .put("thumbnail", item.thumbnail)
                    .put("episode", item.episode ?: -1)
                    .put("watchedAt", item.watchedAt)
                    .put("positionMs", item.positionMs)
                    .put("durationMs", item.durationMs),
            )
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun decode(raw: String?): List<WatchHistoryItem> {
        return runCatching {
            val arr = JSONArray(raw.orEmpty().ifBlank { "[]" })
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val slug = o.optString("slug").trim()
                    if (slug.isEmpty()) continue
                    add(
                        WatchHistoryItem(
                            slug = slug,
                            title = o.optString("title"),
                            thumbnail = o.optString("thumbnail"),
                            episode = o.optEpisodeNumber(),
                            watchedAt = o.optLong("watchedAt", 0L),
                            positionMs = o.optLong("positionMs", 0L).coerceAtLeast(0L),
                            durationMs = o.optLong("durationMs", 0L).coerceAtLeast(0L),
                        ),
                    )
                }
            }.sortedByDescending { it.watchedAt }
        }.getOrDefault(emptyList())
    }

    private fun bump() {
        _revision.value = _revision.value + 1
    }

    companion object {
        private const val PREFS = "weeboonime_watch_history"
        private const val KEY_LEGACY = "items"
        private const val KEY_GUEST = "items__guest"
        private const val MAX = 200
        private const val CLOUD_PUSH_MIN_MS = 15_000L
    }
}

private fun JSONObject.optEpisodeNumber(): Double? {
    if (!has("episode") || isNull("episode")) return null
    return when (val raw = opt("episode")) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    }?.takeIf { it > 0 }
}
