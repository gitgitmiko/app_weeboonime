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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    var showPanel by remember { mutableStateOf(false) }
    var panelTab by remember { mutableStateOf(PanelTab.Server) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

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
        showPanel = false
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
    LaunchedEffect(repeatMode, exoPlayer) {
        exoPlayer?.repeatMode = repeatMode
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            modifier = playerModifier
                .background(Color.Black)
                .statusBarsPadding(),
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
                    val url = players.getOrNull(selectedServer)?.url.orEmpty()
                    if (url.isNotBlank()) {
                        PlaybackSurface(
                            url = url,
                            onPlayerReady = { exoPlayer = it },
                            onPlayerCleared = { if (exoPlayer === it) exoPlayer = null },
                        )
                    }
                }
            }

            // Top chrome
            Row(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 2.dp),
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
                IconButton(onClick = {
                    showPanel = !showPanel
                    if (showPanel) panelTab = PanelTab.Server
                }) {
                    Icon(Icons.Default.Settings, contentDescription = "Pengaturan player", tint = Color.White)
                }
                IconButton(onClick = {
                    fullscreen = !fullscreen
                    showPanel = false
                    onFullscreenChange(fullscreen)
                }) {
                    Icon(
                        if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                    )
                }
            }

            if (showPanel) {
                PlayerSettingsPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    tab = panelTab,
                    onTabChange = { panelTab = it },
                    players = players,
                    selectedServer = selectedServer,
                    onSelectServer = {
                        selectedServer = it
                        showPanel = false
                    },
                    playbackSpeed = playbackSpeed,
                    onSpeed = { playbackSpeed = it },
                    repeatMode = repeatMode,
                    onRepeat = { repeatMode = it },
                    directMedia = players.getOrNull(selectedServer)?.url
                        ?.let { PlayerRouter.isDirectMedia(it) } == true,
                    onDismiss = { showPanel = false },
                )
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
                        listOf(
                            EpisodeSummary(
                                episode = currentEpisode,
                                title = episodeTitle,
                            ),
                        )
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
                                .clickable {
                                    if (n != currentEpisode) currentEpisode = n
                                },
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

private enum class PanelTab { Server, Speed, Repeat }

@Composable
private fun PlayerSettingsPanel(
    modifier: Modifier = Modifier,
    tab: PanelTab,
    onTabChange: (PanelTab) -> Unit,
    players: List<PlayerServer>,
    selectedServer: Int,
    onSelectServer: (Int) -> Unit,
    playbackSpeed: Float,
    onSpeed: (Float) -> Unit,
    repeatMode: Int,
    onRepeat: (Int) -> Unit,
    directMedia: Boolean,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = Color(0xF0121212),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Pengaturan player", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Tutup",
                    color = Color(0xFFB3B3B3),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PanelTabChip("Server", tab == PanelTab.Server, Icons.Default.Settings) {
                    onTabChange(PanelTab.Server)
                }
                PanelTabChip("Speed", tab == PanelTab.Speed, Icons.Default.Speed) {
                    onTabChange(PanelTab.Speed)
                }
                PanelTabChip(
                    "Repeat",
                    tab == PanelTab.Repeat,
                    if (repeatMode == Player.REPEAT_MODE_OFF) Icons.Default.Repeat else Icons.Default.RepeatOne,
                ) {
                    onTabChange(PanelTab.Repeat)
                }
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                PanelTab.Server -> {
                    if (players.isEmpty()) {
                        Text("Tidak ada server", color = Color.White)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(players) { index, server ->
                                FilterChip(
                                    selected = index == selectedServer,
                                    onClick = { onSelectServer(index) },
                                    label = { Text(server.displayLabel()) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF2A2A2A),
                                        labelColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                }
                PanelTab.Speed -> {
                    if (!directMedia) {
                        Text(
                            "Speed hanya untuk server file langsung (bukan embed).",
                            color = Color(0xFFB3B3B3),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SpeedOptions) { speed ->
                            val selected = playbackSpeed == speed
                            FilterChip(
                                selected = selected,
                                onClick = { onSpeed(speed) },
                                enabled = directMedia,
                                label = {
                                    Text(
                                        if (speed == 1f) "1x" else "${speed}x",
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF2A2A2A),
                                    labelColor = Color.White,
                                ),
                            )
                        }
                    }
                }
                PanelTab.Repeat -> {
                    if (!directMedia) {
                        Text(
                            "Repeat hanya untuk server file langsung (bukan embed).",
                            color = Color(0xFFB3B3B3),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = repeatMode == Player.REPEAT_MODE_OFF,
                            onClick = { onRepeat(Player.REPEAT_MODE_OFF) },
                            enabled = directMedia,
                            label = { Text("Off") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2A2A2A),
                                labelColor = Color.White,
                            ),
                        )
                        FilterChip(
                            selected = repeatMode == Player.REPEAT_MODE_ONE,
                            onClick = { onRepeat(Player.REPEAT_MODE_ONE) },
                            enabled = directMedia,
                            label = { Text("Satu video") },
                            leadingIcon = {
                                Icon(Icons.Default.RepeatOne, null)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2A2A2A),
                                labelColor = Color.White,
                            ),
                        )
                        FilterChip(
                            selected = repeatMode == Player.REPEAT_MODE_ALL,
                            onClick = { onRepeat(Player.REPEAT_MODE_ALL) },
                            enabled = directMedia,
                            label = { Text("Loop") },
                            leadingIcon = {
                                Icon(Icons.Default.Repeat, null)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF2A2A2A),
                                labelColor = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelTabChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            containerColor = Color(0xFF2A2A2A),
            labelColor = Color.White,
        ),
    )
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
                it.repeatMode = Player.REPEAT_MODE_OFF
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
                    useController = true
                    controllerShowTimeoutMs = 3500
                    setShowNextButton(false)
                    setShowPreviousButton(false)
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
