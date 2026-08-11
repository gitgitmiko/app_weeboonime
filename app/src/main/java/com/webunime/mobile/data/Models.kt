package com.webunime.mobile.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class HomeResponse(
    val updatedAt: String? = null,
    val latest: List<LatestItem> = emptyList(),
    val movies: List<AnimeCard> = emptyList(),
    val scheduleToday: ScheduleDay? = null,
    val genres: List<String> = emptyList(),
    val newUpdate: List<AnimeCard> = emptyList(),
    val hot: List<AnimeCard> = emptyList(),
    val completed: List<AnimeCard> = emptyList(),
    val selectedGenre: String? = null,
)

@JsonClass(generateAdapter = false)
data class LatestItem(
    val anime_slug: String? = null,
    val slug: String? = null,
    val judul: String? = null,
    val nama: String? = null,
    val episode: Double? = null,
    val thumbnail: String? = null,
    val released_at: String? = null,
    val released_on: String? = null,
) {
    fun displayTitle(): String = judul ?: nama ?: slug ?: anime_slug ?: "Tanpa judul"
    fun catalogSlug(): String = anime_slug ?: slug ?: ""
}

@JsonClass(generateAdapter = false)
data class AnimeCard(
    val slug: String? = null,
    val judul: String? = null,
    val nama: String? = null,
    val thumbnail: String? = null,
    val rating: String? = null,
    val type: String? = null,
    val status: String? = null,
    val genre: List<String>? = null,
    val episodes_count: Int? = null,
    val episode: Double? = null,
) {
    fun displayTitle(): String = judul ?: nama ?: slug ?: "Tanpa judul"
}

@JsonClass(generateAdapter = false)
data class LatestFeedResponse(
    val updatedAt: String? = null,
    val items: List<LatestItem> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class SearchResponse(
    val q: String? = null,
    val count: Int? = null,
    val items: List<AnimeCard> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class CalendarResponse(
    val source: String? = null,
    val scraped_at: String? = null,
    val timezone: String? = null,
    val today: String? = null,
    val days: List<ScheduleDay> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class ScheduleDay(
    val day: String? = null,
    val label: String? = null,
    val items: List<ScheduleItem> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class ScheduleItem(
    val slug: String? = null,
    val judul: String? = null,
    val nama: String? = null,
    val thumbnail: String? = null,
    val rating: String? = null,
    val type: String? = null,
    val genre: List<String>? = null,
    val time: String? = null,
    val source: String? = null,
    val latest_episode: Double? = null,
    val latest_released_at: String? = null,
) {
    fun displayTitle(): String = judul ?: nama ?: slug ?: "Tanpa judul"
}

@JsonClass(generateAdapter = false)
data class AnimeDetail(
    val slug: String? = null,
    val judul: String? = null,
    val nama: String? = null,
    val thumbnail: String? = null,
    val rating: String? = null,
    val type: String? = null,
    val genre: List<String>? = null,
    val sinopsis: String? = null,
    val episodes_count: Int? = null,
    val mal_id: Int? = null,
    val episodes: List<EpisodeSummary> = emptyList(),
) {
    fun displayTitle(): String = judul ?: nama ?: slug ?: "Tanpa judul"
}

@JsonClass(generateAdapter = false)
data class EpisodeSummary(
    val episode: Double? = null,
    val title: String? = null,
    val slug: String? = null,
    val source: String? = null,
    val has_players: Boolean? = null,
    val skip: Any? = null,
)

@JsonClass(generateAdapter = false)
data class EpisodePlayback(
    val slug: String? = null,
    val judul: String? = null,
    val thumbnail: String? = null,
    val episode: EpisodePayload? = null,
)

@JsonClass(generateAdapter = false)
data class EpisodePayload(
    val episode: Double? = null,
    val title: String? = null,
    val slug: String? = null,
    val source: String? = null,
    val players: List<PlayerServer> = emptyList(),
    val skip: Any? = null,
)

@JsonClass(generateAdapter = false)
data class PlayerServer(
    val no: Int? = null,
    val server: String? = null,
    val label: String? = null,
    val url: String? = null,
    @Json(name = "default") val isDefault: Boolean? = null,
) {
    fun displayLabel(): String = label?.takeIf { it.isNotBlank() } ?: server ?: "Server"
}
