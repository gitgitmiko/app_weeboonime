package com.webunime.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.AnimeCard
import com.webunime.mobile.data.HomeResponse
import com.webunime.mobile.data.toEpisodeLabel
import com.webunime.mobile.ui.components.ContinueWatchingSection
import com.webunime.mobile.ui.components.HomePremiumPromo
import com.webunime.mobile.ui.components.HomeUserHeader
import com.webunime.mobile.ui.components.HorizontalWibukuPosterRow
import com.webunime.mobile.ui.components.SectionHeader
import com.webunime.mobile.ui.components.WibukuPosterCard
import com.webunime.mobile.ui.theme.WuColors
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenAnime: (slug: String) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSchedule: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onContinueWatch: (com.webunime.mobile.data.WatchHistoryItem) -> Unit = { onOpenAnime(it.slug) },
    contentPadding: PaddingValues = PaddingValues(),
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    val scope = rememberCoroutineScope()
    val profile by app.userRepository.profileFlow.collectAsStateWithLifecycle(initialValue = null)
    var home by remember { mutableStateOf<HomeResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var newExpanded by remember { mutableStateOf(false) }
    val historyRev by app.watchHistory.revision.collectAsStateWithLifecycle()
    var continueItems by remember { mutableStateOf(app.watchHistory.continueWatching()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(historyRev) {
        continueItems = app.watchHistory.continueWatching()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                continueItems = app.watchHistory.continueWatching()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun reload(genre: String? = selectedGenre) {
        scope.launch {
            loading = true
            error = null
            runCatching { app.catalogApi.home(genre) }
                .onSuccess { home = it }
                .onFailure { error = it.message ?: "Gagal memuat" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload(null) }

    when {
        loading && home == null -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = WuColors.AccentBlue) }

        error != null && home == null -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = WuColors.Muted)
                TextButton(onClick = { reload() }) { Text("Coba lagi") }
            }
        }

        else -> {
            val data = home ?: return
            val newSource = data.newUpdate.ifEmpty {
                data.latest.map {
                    AnimeCard(
                        slug = it.catalogSlug().ifBlank { null },
                        judul = it.displayTitle(),
                        thumbnail = it.thumbnail,
                        episode = it.episode,
                    )
                }
            }
            val collapsedCount = 12 // 3x4
            val expandedCount = 30 // 3x10
            val visibleNew = newSource.take(if (newExpanded) expandedCount else collapsedCount)
            val newRows = visibleNew.chunked(3)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WuColors.Bg)
                    .padding(contentPadding),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item {
                    HomeUserHeader(
                        profile = profile,
                        sessionName = app.session.displayName,
                        onClick = onOpenAccount,
                    )
                }

                item {
                    HomePremiumPromo(onOpenPremium = onOpenPremium)
                }

                item {
                    ContinueWatchingSection(
                        items = continueItems,
                        onOpenItem = onContinueWatch,
                        onSeeAll = onOpenHistory,
                    )
                }

                item {
                    Row(
                        Modifier
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(WuColors.SurfaceAlt)
                            .clickable(onClick = onOpenSearch)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Search, null, tint = WuColors.Muted)
                        Text("Cari Anime Di Sini", color = WuColors.Muted, fontSize = 15.sp)
                    }
                }

                item {
                    SectionHeader(
                        title = "New Update Anime",
                        action = "Lihat Jadwal >",
                        onAction = onOpenSchedule,
                    )
                }

                items(newRows.size) { rowIdx ->
                    val row = newRows[rowIdx]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { item ->
                            WibukuPosterCard(
                                title = item.displayTitle(),
                                thumbnail = item.thumbnail,
                                episodeLabel = item.episode?.let { "Eps ${it.toEpisodeLabel()}" }
                                    ?: item.episodes_count?.let { "Eps $it" },
                                rating = item.rating,
                                showNew = true,
                                onClick = { item.slug?.let(onOpenAnime) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }

                if (newSource.size > collapsedCount) {
                    item {
                        TextButton(
                            onClick = { newExpanded = !newExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                        ) {
                            Text(
                                if (newExpanded) "Show less" else "Show more",
                                color = WuColors.Link,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                if (data.genres.isNotEmpty()) {
                    item {
                        Text(
                            text = "Genre",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    item {
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GenreChip(
                                label = "All",
                                selected = selectedGenre == null,
                                onClick = {
                                    selectedGenre = null
                                    newExpanded = false
                                    reload(null)
                                },
                            )
                            data.genres.take(40).forEach { g ->
                                GenreChip(
                                    label = g,
                                    selected = selectedGenre.equals(g, ignoreCase = true),
                                    onClick = {
                                        selectedGenre = g
                                        newExpanded = false
                                        reload(g)
                                    },
                                )
                            }
                        }
                    }
                }

                if (data.hot.isNotEmpty()) {
                    item { SectionHeader(title = "Hot Anime") }
                    item {
                        HorizontalWibukuPosterRow(
                            items = data.hot,
                            titleOf = { it.displayTitle() },
                            thumbOf = { it.thumbnail },
                            ratingOf = { it.rating },
                            episodeOf = { it.episodes_count?.let { n -> "Eps $n" } },
                            onClick = { it.slug?.let(onOpenAnime) },
                        )
                    }
                }

                if (data.completed.isNotEmpty()) {
                    item { SectionHeader(title = "Completed Anime") }
                    item {
                        HorizontalWibukuPosterRow(
                            items = data.completed,
                            titleOf = { it.displayTitle() },
                            thumbOf = { it.thumbnail },
                            ratingOf = { it.rating },
                            episodeOf = { it.episodes_count?.let { n -> "Eps $n" } },
                            onClick = { it.slug?.let(onOpenAnime) },
                        )
                    }
                }

                if (data.movies.isNotEmpty() && selectedGenre == null) {
                    item { SectionHeader(title = "Anime Movie") }
                    item {
                        HorizontalWibukuPosterRow(
                            items = data.movies,
                            titleOf = { it.displayTitle() },
                            thumbOf = { it.thumbnail },
                            ratingOf = { it.rating },
                            onClick = { it.slug?.let(onOpenAnime) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) WuColors.AccentBlue else WuColors.SurfaceAlt)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
