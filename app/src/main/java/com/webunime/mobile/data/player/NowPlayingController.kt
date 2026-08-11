package com.webunime.mobile.data.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val slug: String,
    val title: String,
    val thumbnail: String,
    val episode: Double,
)

/** Mini-player session (metadata). Tap bar → buka ulang PlayerActivity. */
class NowPlayingController {
    private val _current = MutableStateFlow<NowPlaying?>(null)
    val current: StateFlow<NowPlaying?> = _current.asStateFlow()

    fun set(playing: NowPlaying) {
        _current.value = playing
    }

    fun clear() {
        _current.value = null
    }
}
