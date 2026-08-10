package com.webunime.mobile.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.EpisodeSummary
import com.webunime.mobile.data.PlayerRouter
import com.webunime.mobile.data.PlayerServer
import com.webunime.mobile.ui.theme.WebunimeTheme
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val slug = intent.getStringExtra(EXTRA_SLUG).orEmpty()
        val startEpisode = intent.getIntExtra(EXTRA_EPISODE, 1)
        val animeTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val thumbnail = intent.getStringExtra(EXTRA_THUMBNAIL).orEmpty()
        val app = application as WebunimeApp

        setContent {
            WebunimeTheme {
                PlayerScreen(
                    slug = slug,
                    startEpisode = startEpisode,
                    animeTitle = animeTitle,
                    thumbnail = thumbnail,
                    app = app,
                    onBack = { ep, title, thumb ->
                        app.nowPlaying.set(
                            com.webunime.mobile.data.player.NowPlaying(
                                slug = slug,
                                title = title.ifBlank { animeTitle },
                                thumbnail = thumb.ifBlank { thumbnail },
                                episode = ep,
                            ),
                        )
                        finish()
                    },
                    loadEpisode = { s, ep -> app.catalogApi.episode(s, ep) },
                    loadAnime = { s -> app.catalogApi.anime(s) },
                    onFullscreenChange = { full ->
                        requestedOrientation = if (full) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        setImmersiveFullscreen(full)
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        setImmersiveFullscreen(false)
        super.onDestroy()
    }

    /** Sembunyikan status bar + nav bar saat fullscreen landscape. */
    private fun setImmersiveFullscreen(full: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (full) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_TITLE = "title"
        const val EXTRA_THUMBNAIL = "thumbnail"
    }
}

private val SpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private enum class BottomSheet { None, Speed, Quality }

@Composable
private fun PlayerScreen(
    slug: String,
    startEpisode: Int,
    animeTitle: String,
    thumbnail: String,
    app: WebunimeApp,
    onBack: (episode: Int, title: String, thumbnail: String) -> Unit,
    loadEpisode: suspend (String, Int) -> com.webunime.mobile.data.EpisodePlayback,
    loadAnime: suspend (String) -> com.webunime.mobile.data.AnimeDetail,
    onFullscreenChange: (Boolean) -> Unit,
) {
    var currentEpisode by remember { mutableIntStateOf(startEpisode) }
    var episodes by remember { mutableStateOf<List<EpisodeSummary>>(emptyList()) }
    var title by remember { mutableStateOf(animeTitle) }
    var cover by remember { mutableStateOf(thumbnail) }
    var synopsis by remember { mutableStateOf("") }
    var episodeTitle by remember { mutableStateOf(animeTitle) }
    var players by remember { mutableStateOf<List<PlayerServer>>(emptyList()) }
    var selectedServer by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var fullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var bottomSheet by remember { mutableStateOf(BottomSheet.None) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var autoNext by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val episodeNumbers = remember(episodes, currentEpisode) {
        val nums = episodes.mapNotNull { it.episode }.distinct().sorted()
        if (nums.isEmpty()) listOf(currentEpisode) else nums
    }
    val epIndex = episodeNumbers.indexOf(currentEpisode).coerceAtLeast(0)
    val prevEpisode = episodeNumbers.getOrNull(epIndex - 1)
    val nextEpisode = episodeNumbers.getOrNull(epIndex + 1)

    val currentUrl = players.getOrNull(selectedServer)?.url.orEmpty()

    BackHandler {
        if (fullscreen) {
            fullscreen = false
            onFullscreenChange(false)
        } else {
            val player = exoPlayer
            if (player != null) {
                val pos = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration.coerceAtLeast(0L)
                if (dur > 0L && pos > 1_000L) {
                    app.watchHistory.record(
                        slug = slug,
                        title = title,
                        thumbnail = cover,
                        episode = currentEpisode,
                        positionMs = pos,
                        durationMs = dur,
                    )
                }
            }
            onBack(currentEpisode, title, cover)
        }
    }

    LaunchedEffect(slug) {
        runCatching { loadAnime(slug) }
            .onSuccess { detail ->
                title = detail.displayTitle().ifBlank { animeTitle }
                episodes = detail.episodes
                cover = detail.thumbnail?.takeIf { it.isNotBlank() } ?: thumbnail
                synopsis = detail.sinopsis.orEmpty()
            }
    }

    LaunchedEffect(slug, currentEpisode) {
        loading = true
        error = null
        bottomSheet = BottomSheet.None
        controlsVisible = true
        playbackError = null
        runCatching { loadEpisode(slug, currentEpisode) }
            .onSuccess { payload ->
                episodeTitle = payload.episode?.title
                    ?: payload.judul
                    ?: "Episode $currentEpisode"
                players = PlayerRouter.forPlayback(payload.episode?.players.orEmpty())
                selectedServer = 0
                if (players.isEmpty()) {
                    error = "Tidak ada stream langsung (mp4). Coba episode lain."
                }
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer?.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        var tick = 0
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            isPlaying = player.playWhenReady &&
                player.playbackState != Player.STATE_ENDED &&
                player.playbackState != Player.STATE_IDLE
            tick++
            // ~setiap 5 detik
            if (tick % 12 == 0 && durationMs > 0L && positionMs > 2_000L) {
                app.watchHistory.record(
                    slug = slug,
                    title = title,
                    thumbnail = cover,
                    episode = currentEpisode,
                    positionMs = positionMs,
                    durationMs = durationMs,
                )
            }
            delay(400)
        }
    }

    LaunchedEffect(controlsVisible, bottomSheet, isPlaying) {
        if (!controlsVisible || bottomSheet != BottomSheet.None || !isPlaying) return@LaunchedEffect
        delay(3500)
        controlsVisible = false
    }

    DisposableEffect(exoPlayer, autoNext, nextEpisode, slug, currentEpisode) {
        val player = exoPlayer
        if (player == null) {
            return@DisposableEffect onDispose { }
        }
        var seekDone = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !seekDone) {
                    seekDone = true
                    val saved = app.watchHistory.list()
                        .firstOrNull { it.slug == slug && it.episode == currentEpisode }
                    val pos = saved?.positionMs ?: 0L
                    if (pos > 3_000L && player.duration > 0 && pos < player.duration - 5_000L) {
                        player.seekTo(pos)
                    }
                }
                if (playbackState == Player.STATE_ENDED && autoNext) {
                    nextEpisode?.let { currentEpisode = it }
                }
            }
        }
        player.addListener(listener)
        onDispose {
            val pos = player.currentPosition.coerceAtLeast(0L)
            val dur = player.duration.coerceAtLeast(0L)
            if (dur > 0L && pos > 1_000L) {
                app.watchHistory.record(
                    slug = slug,
                    title = title,
                    thumbnail = cover,
                    episode = currentEpisode,
                    positionMs = pos,
                    durationMs = dur,
                )
            }
            player.removeListener(listener)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (fullscreen) Color.Black else MaterialTheme.colorScheme.background)
            .then(if (fullscreen) Modifier else Modifier.statusBarsPadding()),
    ) {
        val playerModifier = if (fullscreen) {
            Modifier
                .fillMaxSize()
                .weight(1f)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        }

        Box(
            modifier = playerModifier.background(Color.Black),
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                error != null && players.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(error ?: "", color = Color.White, modifier = Modifier.padding(16.dp))
                }
                else -> {
                    if (currentUrl.isNotBlank()) {
                        PlaybackSurface(
                            url = currentUrl,
                            onPlayerReady = { exoPlayer = it },
                            onPlayerCleared = { if (exoPlayer === it) exoPlayer = null },
                            onError = { playbackError = it },
                        )
                    }
                    playbackError?.let { msg ->
                        Text(
                            text = msg,
                            color = Color(0xFFFF8A80),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                        )
                    }
                }
            }

            // Tap kosong: tampilkan kontrol (saat kontrol sembunyi)
            if (!controlsVisible && bottomSheet == BottomSheet.None) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { controlsVisible = true },
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible || bottomSheet != BottomSheet.None,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Scrim: tap area kosong menutup kontrol
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (bottomSheet != BottomSheet.None) {
                                    bottomSheet = BottomSheet.None
                                } else {
                                    controlsVisible = false
                                }
                            },
                    )

                    // Top gradient + back
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent),
                                ),
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { /* block scrim */ },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            if (fullscreen) {
                                fullscreen = false
                                onFullscreenChange(false)
                            } else {
                                onBack(currentEpisode, title, cover)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            text = if (fullscreen) episodeTitle else title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // Center transport
                    Row(
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TransportButton(
                            icon = Icons.Default.SkipPrevious,
                            enabled = prevEpisode != null,
                            size = 40.dp,
                        ) {
                            prevEpisode?.let { currentEpisode = it }
                        }
                        TransportButton(
                            icon = Icons.Default.Replay10,
                            enabled = exoPlayer != null,
                            size = 44.dp,
                        ) {
                            exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                        }
                        IconButton(
                            onClick = {
                                val p = exoPlayer ?: return@IconButton
                                when {
                                    p.playbackState == Player.STATE_IDLE -> {
                                        p.prepare()
                                        p.playWhenReady = true
                                    }
                                    p.playbackState == Player.STATE_ENDED -> {
                                        p.seekTo(0)
                                        p.playWhenReady = true
                                    }
                                    else -> p.playWhenReady = !p.playWhenReady
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White.copy(alpha = 0.22f), CircleShape),
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        TransportButton(
                            icon = Icons.Default.Forward10,
                            enabled = exoPlayer != null,
                            size = 44.dp,
                        ) {
                            exoPlayer?.let {
                                val dur = it.duration.takeIf { d -> d > 0 } ?: Long.MAX_VALUE
                                it.seekTo((it.currentPosition + 10_000).coerceAtMost(dur))
                            }
                        }
                        TransportButton(
                            icon = Icons.Default.SkipNext,
                            enabled = nextEpisode != null,
                            size = 40.dp,
                        ) {
                            nextEpisode?.let { currentEpisode = it }
                        }
                    }

                    // Bottom controls
                    Column(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                ),
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { /* block scrim */ },
                    ) {
                        if (exoPlayer != null && durationMs > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(formatTime(positionMs), color = Color.White, fontSize = 11.sp)
                                Slider(
                                    value = positionMs.toFloat().coerceAtMost(durationMs.toFloat()),
                                    onValueChange = { positionMs = it.toLong() },
                                    onValueChangeFinished = {
                                        exoPlayer?.seekTo(positionMs)
                                    },
                                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFE53935),
                                        activeTrackColor = Color(0xFFE53935),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                    ),
                                )
                                Text(formatTime(durationMs), color = Color.White, fontSize = 11.sp)
                            }
                        }

                        // Right-aligned: AutoNext · Quality · Speed · Fullscreen
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { autoNext = !autoNext },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = if (autoNext) "AutoNext On" else "AutoNext Off",
                                    color = if (autoNext) MaterialTheme.colorScheme.primary else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            TextButton(
                                onClick = {
                                    bottomSheet = if (bottomSheet == BottomSheet.Quality) {
                                        BottomSheet.None
                                    } else {
                                        BottomSheet.Quality
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                val label = players.getOrNull(selectedServer)
                                    ?.let { PlayerRouter.qualityLabel(it) }
                                    ?: "Auto"
                                Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = {
                                    bottomSheet = if (bottomSheet == BottomSheet.Speed) {
                                        BottomSheet.None
                                    } else {
                                        BottomSheet.Speed
                                    }
                                },
                                enabled = exoPlayer != null,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = speedLabel(playbackSpeed),
                                    color = if (exoPlayer != null) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            IconButton(onClick = {
                                fullscreen = !fullscreen
                                bottomSheet = BottomSheet.None
                                onFullscreenChange(fullscreen)
                            }) {
                                Icon(
                                    if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                )
                            }
                        }
                    }

                    if (bottomSheet != BottomSheet.None) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(bottom = 56.dp),
                            color = Color(0xF0141414),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    if (bottomSheet == BottomSheet.Speed) "Kecepatan" else "Resolusi",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Spacer(Modifier.height(8.dp))
                                when (bottomSheet) {
                                    BottomSheet.Speed -> LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(SpeedOptions) { speed ->
                                            FilterChip(
                                                selected = playbackSpeed == speed,
                                                onClick = {
                                                    playbackSpeed = speed
                                                    bottomSheet = BottomSheet.None
                                                },
                                                label = { Text(speedLabel(speed)) },
                                                colors = chipColors(),
                                            )
                                        }
                                    }
                                    BottomSheet.Quality -> {
                                        if (players.isEmpty()) {
                                            Text("Tidak ada pilihan", color = Color.White)
                                        } else {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                itemsIndexed(players) { index, server ->
                                                    FilterChip(
                                                        selected = index == selectedServer,
                                                        onClick = {
                                                            selectedServer = index
                                                            bottomSheet = BottomSheet.None
                                                        },
                                                        label = { Text(PlayerRouter.qualityLabel(server)) },
                                                        colors = chipColors(),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    BottomSheet.None -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!fullscreen) {
            val unlocks by app.episodeUnlocks.unlocksFlow.collectAsStateWithLifecycle(initialValue = emptySet())
            val profile by app.userRepository.profileFlow.collectAsStateWithLifecycle(initialValue = null)
            val isPremium = profile?.effectivePremium() == true
            var synopsisExpanded by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                item {
                    PlayerActionPills(
                        qualityLabel = players.getOrNull(selectedServer)
                            ?.let { PlayerRouter.qualityLabel(it) } ?: "Auto",
                        onQuality = {
                            bottomSheet = if (bottomSheet == BottomSheet.Quality) {
                                BottomSheet.None
                            } else {
                                BottomSheet.Quality
                            }
                            controlsVisible = true
                        },
                    )
                }
                item {
                    PlayerAnimeHeader(
                        title = title,
                        cover = cover,
                        episode = currentEpisode,
                    )
                }
                if (synopsis.isNotBlank()) {
                    item {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            Text(
                                text = synopsis,
                                color = Color(0xFFA0A0A1),
                                fontSize = 13.sp,
                                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (synopsisExpanded) "Sembunyikan" else "Selengkapnya",
                                color = Color(0xFF64B5F6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                                    .padding(top = 4.dp),
                            )
                        }
                    }
                }
                item {
                    Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        com.webunime.mobile.ui.detail.PremiumBanner(onClick = { /* account via reopen */ })
                    }
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Episode List",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(com.webunime.mobile.R.drawable.ic_key),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${profile?.keys ?: 0}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                item {
                    val nums = if (episodes.isEmpty()) {
                        listOf(currentEpisode)
                    } else {
                        episodes.mapNotNull { it.episode }.distinct().sorted()
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(nums) { n ->
                            val unlocked = isPremium ||
                                unlocks.contains(
                                    com.webunime.mobile.data.user.EpisodeUnlockStore.key(slug, n),
                                ) ||
                                n == currentEpisode
                            val selected = n == currentEpisode
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) Color.White else Color(0xFF2B2C2F),
                                    )
                                    .clickable {
                                        if (n == currentEpisode) return@clickable
                                        if (unlocked || isPremium) {
                                            currentEpisode = n
                                        } else {
                                            scope.launch {
                                                val res = app.userRepository.consumeKeyForEpisode(slug, n)
                                                res.onSuccess {
                                                    app.episodeUnlocks.markUnlocked(slug, n)
                                                    runCatching { app.userRepository.grantEpisodeXp() }
                                                    currentEpisode = n
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$n",
                                    color = if (selected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (!unlocked && !selected) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    enabled: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size + 8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(size),
        )
    }
}

@Composable
private fun PlayerActionPills(
    qualityLabel: String,
    onQuality: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(Icons.Default.ThumbUp, "Like")
        Pill(Icons.Default.ThumbDown, "Dislike")
        Pill(Icons.Default.PlayArrow, "$qualityLabel Quality", onClick = onQuality)
        Pill(Icons.Default.Download, "Download")
    }
}

@Composable
private fun Pill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(WuColors.SurfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun PlayerAnimeHeader(
    title: String,
    cover: String,
    episode: Int,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = cover,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(WuColors.SurfaceAlt),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Episode $episode", color = WuColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Visibility, null, tint = WuColors.Muted, modifier = Modifier.size(12.dp))
            }
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(WuColors.AccentYellow)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Flag, null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Report", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = Color.White,
    containerColor = Color(0xFF2A2A2A),
    labelColor = Color.White,
)

private fun speedLabel(speed: Float): String =
    if (speed == 1f) "1x" else String.format(Locale.US, "%gx", speed)

private fun formatTime(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun PlaybackSurface(
    url: String,
    onPlayerReady: (ExoPlayer) -> Unit,
    onPlayerCleared: (ExoPlayer) -> Unit,
    onError: (String?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val player = remember(url) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://samehadaku.care/",
                    "Origin" to "https://samehadaku.care",
                    "Accept" to "*/*",
                ),
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also {
                it.setMediaItem(MediaItem.fromUri(url))
                it.prepare()
                it.playWhenReady = true
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(error.message ?: "Gagal memutar video")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) onError(null)
            }
        }
        player.addListener(listener)
        onPlayerReady(player)
        onDispose {
            player.removeListener(listener)
            onPlayerCleared(player)
            player.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                controllerAutoShow = false
                controllerHideOnTouch = false
                isClickable = false
                isFocusable = false
                isFocusableInTouchMode = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                // Jangan makan gesture Compose (play overlay).
                setOnTouchListener { _, _ -> false }
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
        },
        modifier = Modifier.fillMaxSize(),
    )
}
