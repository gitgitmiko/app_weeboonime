package com.webunime.mobile.data.user

data class UserProfile(
    val uid: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val keys: Int = 0,
    val gems: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val isPremium: Boolean = false,
    val premiumUntilMs: Long = 0L,
    val animeSubs: List<String> = emptyList(),
) {
    fun effectivePremium(now: Long = System.currentTimeMillis()): Boolean =
        isPremium && (premiumUntilMs <= 0L || premiumUntilMs > now)

    fun xpToNextLevel(): Int = level * 50

    fun xpProgress(): Float {
        val need = xpToNextLevel().coerceAtLeast(1)
        return (xp.toFloat() / need).coerceIn(0f, 1f)
    }

    /** Hashtag ID unik stabil dari Firebase UID (contoh: #A1B2C3D4). */
    fun publicTag(): String {
        val id = uid?.takeIf { it.isNotBlank() } ?: return "#GUEST"
        var h = 0x811C9DC5u
        for (c in id) {
            h = h xor c.code.toUInt()
            h *= 0x01000193u
        }
        return "#" + h.toString(16).uppercase().padStart(8, '0').takeLast(8)
    }

    fun canWatchFree(now: Long = System.currentTimeMillis()): Boolean =
        effectivePremium(now) || keys > 0
}

object EconomyRules {
    fun applyXp(profile: UserProfile, gainedXp: Int, gemsPerLevel: Int): UserProfile {
        var xp = profile.xp + gainedXp
        var level = profile.level
        var gems = profile.gems
        var need = level * 50
        while (xp >= need) {
            xp -= need
            level += 1
            gems += gemsPerLevel
            need = level * 50
        }
        return profile.copy(xp = xp, level = level, gems = gems)
    }
}
