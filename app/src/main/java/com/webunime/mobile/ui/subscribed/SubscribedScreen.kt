package com.webunime.mobile.ui.subscribed

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.AnimeCard
import com.webunime.mobile.data.LatestItem
import com.webunime.mobile.ui.components.WibukuPosterCard
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private enum class SubFilter { All, NewUpdate }

@Composable
fun SubscribedScreen(
    onOpenAnime: (slug: String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    val subs by app.userRepository.animeSubsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var cards by remember { mutableStateOf<List<AnimeCard>>(emptyList()) }
    var latestBySlug by remember { mutableStateOf<Map<String, LatestItem>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(SubFilter.All) }
    var filterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(subs) {
        if (subs.isEmpty()) {
            cards = emptyList()
            latestBySlug = emptyMap()
            return@LaunchedEffect
        }
        loading = true
        val latestMap = runCatching {
            app.catalogApi.latest().items
                .mapNotNull { item ->
                    val s = item.catalogSlug().ifBlank { return@mapNotNull null }
                    s to item
                }
                .toMap()
        }.getOrDefault(emptyMap())
        latestBySlug = latestMap

        cards = coroutineScope {
            subs.map { slug ->
                async {
                    runCatching { app.catalogApi.anime(slug) }.getOrNull()?.let { d ->
                        AnimeCard(
                            slug = d.slug ?: slug,
                            judul = d.judul ?: d.nama,
                            thumbnail = d.thumbnail,
                            rating = d.rating,
                            type = d.type,
                            genre = d.genre,
                            episodes_count = d.episodes_count,
                            episode = latestMap[slug]?.episode,
                        )
                    } ?: AnimeCard(slug = slug, judul = slug)
                }
            }.awaitAll().filterNotNull()
        }
        loading = false
    }

    val visible = when (filter) {
        SubFilter.All -> cards
        SubFilter.NewUpdate -> cards.filter { c ->
            val s = c.slug.orEmpty()
            s.isNotEmpty() && latestBySlug.containsKey(s)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(WuColors.Bg)
            .padding(contentPadding),
    ) {
        Text(
            text = "Subscribed Anime",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 14.dp, bottom = 16.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Total (${visible.size})", color = Color.White, fontSize = 14.sp)
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { filterMenu = true },
                ) {
                    Text(
                        when (filter) {
                            SubFilter.All -> "Semua"
                            SubFilter.NewUpdate -> "New Update"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
                DropdownMenu(expanded = filterMenu, onDismissRequest = { filterMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Semua") },
                        onClick = {
                            filter = SubFilter.All
                            filterMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("New Update") },
                        onClick = {
                            filter = SubFilter.NewUpdate
                            filterMenu = false
                        },
                    )
                }
            }
        }

        when {
            loading && cards.isEmpty() && subs.isNotEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = WuColors.AccentBlue) }

            visible.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (subs.isEmpty()) {
                        "Anime yang kamu subscribe akan muncul di sini, Subscribe dulu animenya sana"
                    } else {
                        "Tidak ada anime dengan update baru di feed saat ini"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 36.dp),
                )
            }

            else -> {
                val rows = visible.chunked(3)
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rows.size) { idx ->
                        val row = rows[idx]
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { card ->
                                WibukuPosterCard(
                                    title = card.displayTitle(),
                                    thumbnail = card.thumbnail,
                                    episodeLabel = card.episode?.let { "Eps $it" }
                                        ?: card.episodes_count?.let { "Eps $it" },
                                    rating = card.rating,
                                    showNew = latestBySlug.containsKey(card.slug.orEmpty()),
                                    onClick = { card.slug?.let(onOpenAnime) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
