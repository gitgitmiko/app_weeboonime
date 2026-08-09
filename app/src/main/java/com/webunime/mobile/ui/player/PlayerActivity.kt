package com.webunime.mobile.ui.player

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.EpisodeSummary
import com.webunime.mobile.data.PlayerRouter
import com.webunime.mobile.data.PlayerServer
import com.webunime.mobile.ui.theme.WebunimeTheme
import kotlinx.coroutines.delay
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
        val app = application as WebunimeApp

        setContent {
            WebunimeTheme {
                PlayerScreen(
                    slug = slug,
                    startEpisode = startEpisode,
                    animeTitle = animeTitle,
                    onBack = { finish() },
                    loadEpisode = { s, ep -> app.catalogApi.episode(s, ep) },
                    loadAnime = { s -> app.catalogApi.anime(s) },
                    onFullscreenChange = { full ->
                        requestedOrientation = if (full) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_SLUG = "slug"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_TITLE = "title"
    }
}

private val SpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private enum class BottomSheet { None, Speed, Quality }

@Composable
private fun PlayerScreen(
    slug: String,
    startEpisode: Int,
    animeTitle: String,
    onBack: () -> Unit,
    loadEpisode: suspend (String, Int) -> com.webunime.mobile.data.EpisodePlayback,
    loadAnime: suspend (String) -> com.webunime.mobile.data.AnimeDetail,
    onFullscreenChange: (Boolean) -> Unit,
) {
    var currentEpisode by remember { mutableIntStateOf(startEpisode) }
    var episodes by remember { mutableStateOf<List<EpisodeSummary>>(emptyList()) }
    var title by remember { mutableStateOf(animeTitle) }
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

    val episodeNumbers = remember(episodes, currentEpisode) {
        val nums = episodes.mapNotNull { it.episode }.distinct().sorted()
        if (nums.isEmpty()) listOf(currentEpisode) else nums
    }
    val epIndex = episodeNumbers.indexOf(currentEpisode).coerceAtLeast(0)
    val prevEpisode = episodeNumbers.getOrNull(epIndex - 1)
    val nextEpisode = episodeNumbers.getOrNull(epIndex + 1)

    val currentUrl = players.getOrNull(selectedServer)?.url.orEmpty()
    val directMedia = currentUrl.isNotBlank() && PlayerRouter.isDirectMedia(currentUrl)

    LaunchedEffect(slug) {
        runCatching { loadAnime(slug) }
            .onSuccess { detail ->
                title = detail.displayTitle().ifBlank { animeTitle }
                episodes = detail.episodes
            }
    }

    LaunchedEffect(slug, currentEpisode) {
        loading = true
        error = null
        bottomSheet = BottomSheet.None
        controlsVisible = true
        runCatching { loadEpisode(slug, currentEpisode) }
            .onSuccess { payload ->
                episodeTitle = payload.episode?.title
                    ?: payload.judul
                    ?: "Episode $currentEpisode"
                players = PlayerRouter.preferred(payload.episode?.players.orEmpty())
                selectedServer = 0
                if (players.isEmpty()) error = "Tidak ada server player"
            }
            .onFailure { error = it.message }
        loading = false
    }

    LaunchedEffect(playbackSpeed, exoPlayer) {
        exoPlayer?.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(400)
        }
    }

    LaunchedEffect(controlsVisible, bottomSheet, isPlaying) {
        if (!controlsVisible || bottomSheet != BottomSheet.None || !isPlaying) return@LaunchedEffect
        delay(3500)
        controlsVisible = false
    }

    DisposableEffect(exoPlayer, autoNext, nextEpisode) {
        val player = exoPlayer
        if (player == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && autoNext) {
                    nextEpisode?.let { currentEpisode = it }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
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
                        )
                    }
                }
            }

            // Tap area to toggle controls
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
                            controlsVisible = !controlsVisible
                        }
                    },
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = controlsVisible || bottomSheet != BottomSheet.None,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(Modifier.fillMaxSize()) {
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
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            if (fullscreen) {
                                fullscreen = false
                                onFullscreenChange(false)
                            } else {
                                onBack()
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
                        Modifier.align(Alignment.Center),
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
                            enabled = directMedia && exoPlayer != null,
                            size = 44.dp,
                        ) {
                            exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.18f),
                            modifier = Modifier
                                .size(64.dp)
                                .clickable(enabled = directMedia && exoPlayer != null) {
                                    exoPlayer?.let { p ->
                                        if (p.isPlaying) p.pause() else p.play()
                                    }
                                },
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        TransportButton(
                            icon = Icons.Default.Forward10,
                            enabled = directMedia && exoPlayer != null,
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        if (directMedia && durationMs > 0) {
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
                                        thumbColor = Color.White,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
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
                                enabled = directMedia,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = speedLabel(playbackSpeed),
                                    color = if (directMedia) Color.White else Color.White.copy(alpha = 0.4f),
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Text(
                    text = episodeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Episode",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    val list = if (episodes.isEmpty()) {
                        listOf(EpisodeSummary(episode = currentEpisode, title = episodeTitle))
                    } else {
                        episodes.asReversed()
                    }
                    items(list, key = { it.episode ?: it.title ?: it.hashCode() }) { ep ->
                        val n = ep.episode ?: return@items
                        val selected = n == currentEpisode
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { if (n != currentEpisode) currentEpisode = n },
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = ep.title?.takeIf { it.isNotBlank() } ?: "Episode $n",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Text(
                                        "Sedang diputar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
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
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val direct = remember(url) { PlayerRouter.isDirectMedia(url) }

    if (direct) {
        val player = remember(url) {
            ExoPlayer.Builder(context).build().also {
                it.setMediaItem(MediaItem.fromUri(url))
                it.prepare()
                it.playWhenReady = true
            }
        }
        DisposableEffect(player) {
            onPlayerReady(player)
            onDispose {
                onPlayerCleared(player)
                player.release()
            }
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player
            },
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            update = { it.loadUrl(url) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
