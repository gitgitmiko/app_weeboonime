package com.webunime.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WatchHistoryItem(
    val slug: String,
    val title: String,
    val thumbnail: String,
    val episode: Int?,
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
 * Riwayat menonton lokal (SharedPreferences).
 * Satu entri per (slug + episode); progress dari player.
 */
class WatchHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(): List<WatchHistoryItem> {
        val raw = prefs.getString(KEY, "[]").orEmpty()
        return runCatching {
            val arr = JSONArray(raw)
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
                            episode = o.optInt("episode", -1).takeIf { it > 0 },
                            watchedAt = o.optLong("watchedAt", 0L),
                            positionMs = o.optLong("positionMs", 0L).coerceAtLeast(0L),
                            durationMs = o.optLong("durationMs", 0L).coerceAtLeast(0L),
                        ),
                    )
                }
            }.sortedByDescending { it.watchedAt }
        }.getOrDefault(emptyList())
    }

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
        episode: Int?,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
    ) {
        if (slug.isBlank()) return
        val now = System.currentTimeMillis()
        val rest = list().filterNot { it.slug == slug && it.episode == episode }
        val prev = list().firstOrNull { it.slug == slug && it.episode == episode }
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
    }

    fun remove(slug: String, episode: Int?) {
        save(list().filterNot { it.slug == slug && it.episode == episode })
    }

    fun removeAll(keys: Set<Pair<String, Int?>>) {
        if (keys.isEmpty()) return
        save(list().filterNot { (it.slug to it.episode) in keys })
    }

    fun clear() {
        prefs.edit().putString(KEY, "[]").apply()
    }

    private fun save(items: List<WatchHistoryItem>) {
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
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "weeboonime_watch_history"
        private const val KEY = "items"
        private const val MAX = 200
    }
}
