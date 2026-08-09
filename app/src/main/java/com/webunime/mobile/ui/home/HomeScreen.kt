package com.webunime.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.HomeResponse
import com.webunime.mobile.ui.components.HorizontalWibukuPosterRow
import com.webunime.mobile.ui.components.SectionHeader
import com.webunime.mobile.ui.components.WibukuPosterCard
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenAnime: (slug: String) -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSchedule: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    val scope = rememberCoroutineScope()
    var home by remember { mutableStateOf<HomeResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.catalogApi.home() }
                .onSuccess { home = it }
                .onFailure { error = it.message ?: "Gagal memuat" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WuColors.Bg)
                    .padding(contentPadding),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
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
                        Text(
                            "Cari Anime Di Sini",
                            color = WuColors.Muted,
                            fontSize = 15.sp,
                        )
                    }
                }

                if (data.latest.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "New Update Anime",
                            action = "Lihat Jadwal >",
                            onAction = onOpenSchedule,
                        )
                    }
                    val rows = data.latest.chunked(3)
                    items(rows) { row ->
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
                                    episodeLabel = item.episode?.let { "Eps $it" },
                                    rating = null,
                                    viewsLabel = null,
                                    showNew = true,
                                    onClick = {
                                        val slug = item.catalogSlug()
                                        if (slug.isNotBlank()) onOpenAnime(slug)
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - row.size) {
                                Box(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (data.movies.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Anime Movie")
                    }
                    item {
                        HorizontalWibukuPosterRow(
                            items = data.movies,
                            titleOf = { it.displayTitle() },
                            thumbOf = { it.thumbnail },
                            ratingOf = { it.rating },
                            episodeOf = { it.episodes_count?.let { n -> "Eps $n" } },
                            onClick = { it.slug?.let(onOpenAnime) },
                        )
                    }
                }

                data.scheduleToday?.takeIf { it.items.isNotEmpty() }?.let { day ->
                    item {
                        SectionHeader(
                            title = "Jadwal ${day.label ?: "Hari Ini"}",
                            action = "Lihat Jadwal >",
                            onAction = onOpenSchedule,
                        )
                    }
                    item {
                        HorizontalWibukuPosterRow(
                            items = day.items,
                            titleOf = { it.displayTitle() },
                            thumbOf = { it.thumbnail },
                            episodeOf = { it.time },
                            ratingOf = { it.rating },
                            onClick = { it.slug?.let(onOpenAnime) },
                        )
                    }
                }
            }
        }
    }
}
