package com.webunime.mobile.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class CatalogApi(baseUrl: String) {
    private val base = baseUrl.trimEnd('/')

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(75, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val homeAdapter = moshi.adapter(HomeResponse::class.java)
    private val searchAdapter = moshi.adapter(SearchResponse::class.java)
    private val calendarAdapter = moshi.adapter(CalendarResponse::class.java)
    private val detailAdapter = moshi.adapter(AnimeDetail::class.java)
    private val episodeAdapter = moshi.adapter(EpisodePlayback::class.java)

    suspend fun home(genre: String? = null): HomeResponse {
        val q = if (!genre.isNullOrBlank()) {
            "?genre=${java.net.URLEncoder.encode(genre, Charsets.UTF_8.name())}"
        } else ""
        return get("/v1/home$q", homeAdapter)
    }

    suspend fun search(q: String, limit: Int = 30): SearchResponse =
        get("/v1/search?q=${java.net.URLEncoder.encode(q, Charsets.UTF_8.name())}&limit=$limit", searchAdapter)

    suspend fun calendar(): CalendarResponse = get("/v1/calendar", calendarAdapter)

    suspend fun anime(slug: String): AnimeDetail =
        get("/v1/anime/${enc(slug)}", detailAdapter)

    suspend fun episode(slug: String, n: Int): EpisodePlayback =
        get("/v1/anime/${enc(slug)}/episodes/$n", episodeAdapter)

    private fun enc(s: String): String =
        java.net.URLEncoder.encode(s, Charsets.UTF_8.name()).replace("+", "%20")

    private suspend fun <T> get(path: String, adapter: com.squareup.moshi.JsonAdapter<T>): T =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$base$path")
                .header("User-Agent", "WEBUNIME-Mobile/0.1")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    error("HTTP ${res.code}: ${body.take(200)}")
                }
                adapter.fromJson(body) ?: error("Empty JSON for $path")
            }
        }
}
