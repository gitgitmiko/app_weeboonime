package com.webunime.mobile.data.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.webunime.mobile.BuildConfig
import com.webunime.mobile.data.toEpisodeKey
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class PlaybackReportReason(val id: String, val label: String) {
    NO_MP4("no_mp4", "Tidak ada video / MP4"),
    PLAYBACK_FAIL("playback_fail", "Video error / tidak bisa diputar"),
    WRONG_EPISODE("wrong_episode", "Episode salah"),
    OTHER("other", "Lainnya"),
    ;

    val requestsRepair: Boolean
        get() = this == NO_MP4 || this == PLAYBACK_FAIL

    companion object {
        fun fromId(id: String): PlaybackReportReason =
            entries.firstOrNull { it.id == id } ?: OTHER
    }
}

data class PlaybackReportDraft(
    val slug: String,
    val title: String,
    val episode: Double,
    val reason: PlaybackReportReason,
    val playerCount: Int,
    val hasDirectMp4: Boolean,
    val playbackError: String?,
    val selectedQuality: String?,
)

class PlaybackReportException(message: String) : Exception(message)

/**
 * Laporan player per UID + episode.
 * Masuk antrean harian; scrape spesifik jalan tengah malam (bukan langsung).
 */
class PlaybackReportRepository {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val db: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun submit(draft: PlaybackReportDraft): String {
        val firestore = db ?: throw PlaybackReportException("Laporan belum siap")
        val user = auth?.currentUser ?: throw PlaybackReportException("Login dulu untuk report")
        val slug = draft.slug.trim().lowercase()
        if (slug.isEmpty()) throw PlaybackReportException("Anime tidak dikenali")

        val id = docId(user.uid, slug, draft.episode)
        val payload = hashMapOf<String, Any>(
            "uid" to user.uid,
            "slug" to slug,
            "title" to draft.title.trim().take(180),
            "episode" to draft.episode,
            "reason" to draft.reason.id,
            "playerCount" to draft.playerCount.coerceIn(0, 50),
            "hasDirectMp4" to draft.hasDirectMp4,
            "playbackError" to (draft.playbackError?.trim()?.take(280) ?: ""),
            "selectedQuality" to (draft.selectedQuality?.trim()?.take(40) ?: ""),
            "appVersion" to BuildConfig.VERSION_NAME,
            "reportedAt" to FieldValue.serverTimestamp(),
        )
        try {
            firestore.collection(COL).document(id).set(payload).await()
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            if (msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                throw PlaybackReportException("Episode ini sudah kamu laporkan. Coba lagi nanti.")
            }
            throw PlaybackReportException(e.message ?: "Gagal kirim laporan")
        }

        val queued = requestRepair(slug, draft.episode, draft.reason.id, draft.title)
        return if (queued) {
            "Laporan terkirim. Episode ini masuk antrean refresh malam ini."
        } else {
            "Laporan terkirim. Terima kasih."
        }
    }

    private suspend fun requestRepair(
        slug: String,
        episode: Double,
        reason: String,
        title: String,
    ): Boolean {
        val token = runCatching {
            auth?.currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull().orEmpty()
        if (token.isBlank()) return false
        val body = JSONObject()
            .put("slug", slug)
            .put("episode", episode.toEpisodeKey())
            .put("reason", reason)
            .put("title", title)
            .toString()
            .toRequestBody(JSON)
        val req = Request.Builder()
            .url("${BuildConfig.REPAIR_API_BASE.trimEnd('/')}/api/repair")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        return runCatching {
            http.newCall(req).execute().use { res ->
                res.isSuccessful || res.code == 202 || res.code == 409
            }
        }.getOrDefault(false)
    }

    companion object {
        private const val COL = "playbackReports"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun docId(uid: String, slug: String, episode: Double): String {
            val s = slug.trim().lowercase().replace(Regex("[^a-z0-9_-]+"), "_")
            return "${uid}__${s}__${episode.toEpisodeKey().replace('.', '_')}"
        }

        fun suggestedReason(playerCount: Int, playbackError: String?): PlaybackReportReason = when {
            playerCount <= 0 -> PlaybackReportReason.NO_MP4
            !playbackError.isNullOrBlank() -> PlaybackReportReason.PLAYBACK_FAIL
            else -> PlaybackReportReason.PLAYBACK_FAIL
        }
    }
}
