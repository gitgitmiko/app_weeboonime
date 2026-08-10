package com.webunime.mobile.ui.detail

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.webunime.mobile.BuildConfig
import com.webunime.mobile.R
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.AnimeDetail
import com.webunime.mobile.data.EpisodeSummary
import com.webunime.mobile.data.user.EpisodeUnlockStore
import com.webunime.mobile.ui.player.PlayerActivity
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    slug: String,
    onBack: () -> Unit,
    onGoAccount: () -> Unit = {},
    onOpenPremium: () -> Unit = onGoAccount,
) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val app = context.applicationContext as WebunimeApp
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<AnimeDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showKeysEmpty by remember { mutableStateOf(false) }
    var pendingEpisode by remember { mutableStateOf<Int?>(null) }
    var synopsisExpanded by remember { mutableStateOf(false) }
    var sortDesc by remember { mutableStateOf(true) }
    var xpPopup by remember { mutableStateOf<XpGainInfo?>(null) }
    val profile by app.userRepository.profileFlow.collectAsStateWithLifecycle(initialValue = null)
    val unlocks by app.episodeUnlocks.unlocksFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    val isPremium = profile?.effectivePremium() == true
    val subscribed = profile?.animeSubs?.contains(slug) == true

    fun openPlayer(n: Int, title: String, thumbnail: String) {
        app.nowPlaying.clear()
        app.watchHistory.record(
            slug = slug,
            title = title,
            thumbnail = thumbnail,
            episode = n,
        )
        val i = Intent(context, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_SLUG, slug)
            putExtra(PlayerActivity.EXTRA_EPISODE, n)
            putExtra(PlayerActivity.EXTRA_TITLE, title)
            putExtra(PlayerActivity.EXTRA_THUMBNAIL, thumbnail)
        }
        context.startActivity(i)
    }

    fun tryWatch(n: Int, title: String, thumbnail: String) {
        scope.launch {
            val unlocked = isPremium ||
                unlocks.contains(EpisodeUnlockStore.key(slug, n)) ||
                app.episodeUnlocks.isUnlocked(slug, n)

            if (unlocked) {
                openPlayer(n, title, thumbnail)
                return@launch
            }

            val res = app.userRepository.consumeKeyForEpisode(slug, n)
            res.onSuccess {
                app.episodeUnlocks.markUnlocked(slug, n)
                val afterXp = runCatching { app.userRepository.grantEpisodeXp() }.getOrNull()
                if (afterXp != null) {
                    xpPopup = xpInfoFrom(afterXp, BuildConfig.XP_PER_EPISODE)
                }
                openPlayer(n, title, thumbnail)
            }.onFailure {
                pendingEpisode = n
                showKeysEmpty = true
            }
        }
    }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.catalogApi.anime(slug) }
                .onSuccess { detail = it }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(slug) { reload() }

    Box(
        Modifier
            .fillMaxSize()
            .background(WuColors.Bg),
    ) {
        when {
            loading && detail == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = WuColors.AccentBlue) }

            error != null && detail == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "", color = WuColors.Muted)
                    TextButton(onClick = { reload() }) { Text("Coba lagi") }
                }
            }

            else -> {
                val data = detail ?: return
                val episodes = if (sortDesc) {
                    data.episodes.sortedByDescending { it.episode ?: 0 }
                } else {
                    data.episodes.sortedBy { it.episode ?: 0 }
                }
                val firstEp = data.episodes.minByOrNull { it.episode ?: Int.MAX_VALUE }?.episode
                    ?: episodes.firstOrNull()?.episode

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    item {
                        DetailHero(
                            title = data.displayTitle(),
                            thumbnail = data.thumbnail,
                            onBack = onBack,
                        )
                    }
                    item {
                        Column(Modifier.padding(horizontal = 14.dp)) {
                            Text(
                                data.displayTitle(),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(10.dp))
                            MetaRow(data)
                            Spacer(Modifier.height(10.dp))
                            GenreRow(data.genre.orEmpty())
                            Spacer(Modifier.height(16.dp))
                            ActionButtons(
                                subscribed = subscribed,
                                onPlay = {
                                    val n = firstEp ?: return@ActionButtons
                                    tryWatch(n, data.displayTitle(), data.thumbnail.orEmpty())
                                },
                                onSubscribe = {
                                    scope.launch {
                                        val nowOn = app.userRepository.toggleSubscribe(slug)
                                        if (nowOn) {
                                            com.webunime.mobile.data.fcm.FcmTopicManager.subscribeSlug(slug)
                                        } else {
                                            com.webunime.mobile.data.fcm.FcmTopicManager.unsubscribeSlug(slug)
                                        }
                                        Toast.makeText(
                                            context,
                                            if (nowOn) "Subscribe aktif — notifikasi episode baru" else "Unsubscribe",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                            )
                            Spacer(Modifier.height(18.dp))
                            SynopsisBlock(
                                text = data.sinopsis.orEmpty(),
                                expanded = synopsisExpanded,
                                onToggle = { synopsisExpanded = !synopsisExpanded },
                            )
                            Spacer(Modifier.height(14.dp))
                            PremiumBanner(onClick = onOpenPremium)
                            Spacer(Modifier.height(18.dp))
                            EpisodesHeader(
                                count = data.episodes.size,
                                sortDesc = sortDesc,
                                onToggleSort = { sortDesc = !sortDesc },
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    items(episodes, key = { it.episode ?: it.hashCode() }) { ep ->
                        val n = ep.episode ?: return@items
                        val unlocked = isPremium ||
                            unlocks.contains(EpisodeUnlockStore.key(slug, n))
                        EpisodeRow(
                            episode = ep,
                            unlocked = unlocked,
                            onOpen = {
                                tryWatch(n, data.displayTitle(), data.thumbnail.orEmpty())
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        FloatingKeysPill(
            keys = profile?.keys ?: 0,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
        )

        if (showKeysEmpty) {
            KeysEmptyDialog(
                onBuyPremium = {
                    showKeysEmpty = false
                    pendingEpisode = null
                    onOpenPremium()
                },
                onWatchAd = {
                    scope.launch {
                        val ok = app.rewardedAds.show(activity)
                        if (ok) {
                            app.userRepository.grantKeys(1)
                            val ep = pendingEpisode
                            val data = detail
                            showKeysEmpty = false
                            pendingEpisode = null
                            if (ep != null && data != null) {
                                tryWatch(ep, data.displayTitle(), data.thumbnail.orEmpty())
                            }
                        } else {
                            app.rewardedAds.preload()
                            Toast.makeText(context, "Iklan belum siap, coba lagi", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = {
                    showKeysEmpty = false
                    pendingEpisode = null
                },
            )
        }

        xpPopup?.let { info ->
            XpGainPopup(info = info, onDismiss = { xpPopup = null })
        }
    }
}

@Composable
private fun DetailHero(
    title: String,
    thumbnail: String?,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.45f to Color.Transparent,
                        1f to WuColors.Bg,
                    ),
                ),
        )
        Row(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetaRow(data: AnimeDetail) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        data.rating?.takeIf { it.isNotBlank() }?.let { rating ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(WuColors.SurfaceAlt)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Star, null, tint = WuColors.AccentYellow, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(rating, color = Color.White, fontSize = 12.sp)
            }
        }
        data.type?.takeIf { it.isNotBlank() }?.let { Chip(it) }
        data.episodes_count?.let { Chip("$it Eps") }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WuColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun GenreRow(genres: List<String>) {
    if (genres.isEmpty()) return
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        genres.take(8).forEach { g ->
            Text(
                g,
                color = Color(0xFFFF8A80),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33E53935))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ActionButtons(
    subscribed: Boolean,
    onPlay: () -> Unit,
    onSubscribe: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .weight(1.15f)
                .clip(RoundedCornerShape(28.dp))
                .background(WuColors.AccentBlue)
                .clickable(onClick = onPlay)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = WuColors.AccentBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Mulai Tonton", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Column(Modifier.weight(0.85f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(WuColors.SurfaceAlt)
                    .clickable(onClick = onSubscribe)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.NotificationsNone,
                    null,
                    tint = if (subscribed) WuColors.AccentYellow else Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (subscribed) "Subscribed" else "Subscribe",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun SynopsisBlock(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    if (text.isBlank()) return
    Column {
        Text("Synopsis", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            color = WuColors.Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier
                .clickable(onClick = onToggle)
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (expanded) "Sembunyikan" else "Selengkapnya",
                color = WuColors.AccentBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                null,
                tint = WuColors.AccentBlue,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EpisodesHeader(
    count: Int,
    sortDesc: Boolean,
    onToggleSort: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Episodes ($count)",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(WuColors.SurfaceAlt)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.GridView, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Grid", color = Color.White, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(WuColors.SurfaceAlt)
                .clickable(onClick = onToggleSort)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (sortDesc) "Sort: 99 ▼ 1" else "Sort: 1 ▲ 99",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: EpisodeSummary,
    unlocked: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = episode.episode ?: return
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(WuColors.Surface)
                .clickable(enabled = unlocked, onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text("Episode $n", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, null, tint = WuColors.Muted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("—", color = WuColors.Muted, fontSize = 12.sp)
                Spacer(Modifier.width(12.dp))
                if (!unlocked) {
                    Icon(Icons.Default.Lock, null, tint = WuColors.Muted, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (!unlocked) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Buka", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_key),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
