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
)

/**
 * Riwayat menonton lokal (SharedPreferences).
 * Dipakai tab History ala Wibuku.
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
                        ),
                    )
                }
            }.sortedByDescending { it.watchedAt }
        }.getOrDefault(emptyList())
    }

    fun record(
        slug: String,
        title: String,
        thumbnail: String,
        episode: Int?,
    ) {
        if (slug.isBlank()) return
        val now = System.currentTimeMillis()
        val rest = list().filterNot { it.slug == slug }
        val next = listOf(
            WatchHistoryItem(
                slug = slug,
                title = title.ifBlank { slug },
                thumbnail = thumbnail,
                episode = episode,
                watchedAt = now,
            ),
        ) + rest
        save(next.take(MAX))
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
                    .put("watchedAt", item.watchedAt),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val PREFS = "weeboonime_watch_history"
        private const val KEY = "items"
        private const val MAX = 100
    }
}
