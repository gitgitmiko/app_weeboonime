package com.webunime.mobile.data

/**
 * Pilih stream ala Wibuku: file langsung (mp4/m3u8), satu opsi per resolusi.
 */
object PlayerRouter {

    enum class Quality(val label: String, val rank: Int) {
        Q1080("1080p", 0),
        Q720("720p", 1),
        Q480("480p", 2),
        Q320("320p", 3),
    }

    fun preferred(players: List<PlayerServer>): List<PlayerServer> = forPlayback(players)

    /** Satu stream langsung per 1080 / 720 / 480 / 320. */
    fun forPlayback(players: List<PlayerServer>): List<PlayerServer> {
        val direct = players
            .filter { !it.url.isNullOrBlank() && isDirectMedia(it.url!!) }
            .sortedWith(compareBy({ sourceScore(it) }, { qualityOf(it)?.rank ?: 99 }))

        val picked = linkedMapOf<Quality, PlayerServer>()
        for (p in direct) {
            val q = qualityOf(p) ?: continue
            if (!picked.containsKey(q)) picked[q] = p
        }

        // Jika label tidak kebaca tapi ada direct, tetap tampilkan (maks 4).
        if (picked.isEmpty() && direct.isNotEmpty()) {
            return direct.distinctBy { it.url }.take(4)
        }

        return picked.entries
            .sortedBy { it.key.rank }
            .map { (q, server) ->
                server.copy(label = q.label)
            }
    }

    fun pickDefault(players: List<PlayerServer>): PlayerServer? =
        forPlayback(players).firstOrNull()

    fun qualityLabel(p: PlayerServer): String =
        qualityOf(p)?.label
            ?: p.label?.takeIf { it.contains(Regex("\\d{3,4}")) }
            ?: "Auto"

    fun isDirectMedia(url: String): Boolean {
        val u = url.lowercase()
        if (u.contains("/embed") ||
            u.contains("abyssplayer") ||
            u.contains("gn1r5n") ||
            u.contains("turbo") ||
            u.contains("emturbovid") ||
            u.contains("blogger.com") ||
            u.contains("mega.nz") ||
            u.contains("filedon.co") ||
            u.contains("api.wibufile.com/embed") ||
            u.contains("login.wibufile.com")
        ) {
            return false
        }
        return u.contains(".mp4") ||
            u.contains(".m3u8") ||
            u.contains(".webm") ||
            u.contains("wibufile.com/video")
    }

    fun qualityOf(p: PlayerServer): Quality? {
        val t = listOfNotNull(p.label, p.server, p.url).joinToString(" ").lowercase()
        return when {
            t.contains("1080") || t.contains("fullhd") || t.contains("full-hd") -> Quality.Q1080
            t.contains("720") || t.contains("mp4hd") -> Quality.Q720
            t.contains("480") -> Quality.Q480
            t.contains("360") || t.contains("320") -> Quality.Q320
            else -> null
        }
    }

    private fun sourceScore(p: PlayerServer): Int {
        val u = (p.url ?: "").lowercase()
        val l = (p.label ?: "").lowercase()
        val s = (p.server ?: "").lowercase()
        val isWibu = u.contains("wibufile") || s.contains("wibu") || l.contains("wibufile")
        return when {
            isWibu -> 10
            u.contains(".mp4") || u.contains(".m3u8") -> 20
            else -> 50
        }
    }
}
