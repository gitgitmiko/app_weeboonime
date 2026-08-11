package com.webunime.mobile.data

import android.content.Intent
import kotlin.math.abs

/** Label UI / kunci unlock: 12.0 → "12", 12.1 → "12.1" */
fun Double.toEpisodeLabel(): String {
    val asLong = toLong()
    return if (abs(this - asLong.toDouble()) < 1e-9) asLong.toString() else toString()
}

fun Double.toEpisodeKey(): String = toEpisodeLabel()

fun Intent.putEpisodeExtra(name: String, episode: Double) {
    putExtra(name, episode)
}

fun Intent.getEpisodeExtra(name: String, default: Double = 1.0): Double {
    val raw = extras?.get(name) ?: return default
    return when (raw) {
        is Double -> raw
        is Float -> raw.toDouble()
        is Int -> raw.toDouble()
        is Long -> raw.toDouble()
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull() ?: default
        else -> default
    }.takeIf { it > 0 } ?: default
}
