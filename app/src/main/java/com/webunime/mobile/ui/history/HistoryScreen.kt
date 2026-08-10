package com.webunime.mobile.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.WatchHistoryItem
import com.webunime.mobile.ui.theme.WuColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onOpenAnime: (slug: String) -> Unit,
    onContinue: (WatchHistoryItem) -> Unit = { onOpenAnime(it.slug) },
    contentPadding: PaddingValues = PaddingValues(),
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    var items by remember { mutableStateOf(app.watchHistory.list()) }
    var multi by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<Pair<String, Int?>>>(emptySet()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) items = app.watchHistory.list()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dayGroups = remember(items) { groupByDayLabel(items) }

    Box(
        Modifier
            .fillMaxSize()
            .background(WuColors.Bg)
            .padding(contentPadding),
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "Riwayat Menonton",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp),
            )
            Text(
                text = if (selecting) "${selected.size} dipilih" else "Tap tahan untuk memilih & hapus",
                color = WuColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 8.dp),
            )

            if (selecting) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = {
                        selecting = false
                        selected = emptySet()
                    }) { Text("Batal") }
                    TextButton(
                        onClick = {
                            app.watchHistory.removeAll(selected)
                            items = app.watchHistory.list()
                            selecting = false
                            selected = emptySet()
                        },
                        enabled = selected.isNotEmpty(),
                    ) { Text("Hapus") }
                }
            }

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Belum ada riwayat", color = WuColors.Muted, fontSize = 16.sp)
                        Text(
                            "Anime yang kamu tonton akan muncul di sini",
                            color = WuColors.Muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                ) {
                    dayGroups.forEach { (day, dayItems) ->
                        item(key = "day-$day") {
                            DayChip(label = day, highlight = day == "Hari ini")
                        }
                        if (multi) {
                            val byAnime = dayItems.groupBy { it.slug }
                            byAnime.forEach { (_, animeItems) ->
                                item(key = "multi-head-${animeItems.first().slug}-$day") {
                                    MultiAnimeHeader(animeItems.first())
                                }
                                items(
                                    animeItems,
                                    key = { "m-${it.slug}-${it.episode}-${it.watchedAt}" },
                                ) { item ->
                                    val key = item.slug to item.episode
                                    MultiEpisodeRow(
                                        item = item,
                                        selected = key in selected,
                                        onClick = {
                                            if (selecting) {
                                                selected =
                                                    if (key in selected) selected - key else selected + key
                                            } else {
                                                onContinue(item)
                                            }
                                        },
                                        onLongClick = {
                                            selecting = true
                                            selected = selected + key
                                        },
                                    )
                                }
                            }
                        } else {
                            items(
                                dayItems,
                                key = { "s-${it.slug}-${it.episode}-${it.watchedAt}" },
                            ) { item ->
                                val key = item.slug to item.episode
                                SingleHistoryCard(
                                    item = item,
                                    selected = key in selected,
                                    onClick = {
                                        if (selecting) {
                                            selected =
                                                if (key in selected) selected - key else selected + key
                                        } else {
                                            onContinue(item)
                                        }
                                    },
                                    onLongClick = {
                                        selecting = true
                                        selected = selected + key
                                    },
                                )
                            }
                        }
                    }
                    if (!selecting) {
                        item {
                            TextButton(
                                onClick = {
                                    app.watchHistory.clear()
                                    items = emptyList()
                                },
                                modifier = Modifier.padding(top = 8.dp),
                            ) { Text("Hapus semua riwayat") }
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(WuColors.SurfaceAlt),
        ) {
            ModeChip("SINGLE", selected = !multi) { multi = false }
            ModeChip("MULTI", selected = multi) { multi = true }
        }
    }
}

@Composable
private fun DayChip(label: String, highlight: Boolean) {
    Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp)) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (highlight) WuColors.AccentBlue else WuColors.SurfaceAlt)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) WuColors.AccentBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleHistoryCard(
    item: WatchHistoryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val time = remember(item.watchedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.watchedAt))
    }
    val progress = item.progressFraction().coerceIn(0.02f, 1f)
    val watched = formatMs(item.positionMs)
    val total = if (item.durationMs > 0) formatMs(item.durationMs) else "--:--"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WuColors.Surface)
            .then(
                if (selected) Modifier.border(1.dp, WuColors.AccentBlue, RoundedCornerShape(12.dp))
                else Modifier,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 52.dp, height = 72.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    item.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(time, color = WuColors.Muted, fontSize = 12.sp)
            }
            Text(
                item.episode?.let { "Episode $it" } ?: "Episode",
                color = WuColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WuColors.Progress,
                trackColor = WuColors.SurfaceAlt,
            )
            Text(
                "$watched / $total",
                color = WuColors.Muted,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun MultiAnimeHeader(item: WatchHistoryItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(WuColors.SurfaceAlt),
            )
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(WuColors.Muted),
            )
        }
        Spacer(Modifier.width(8.dp))
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiEpisodeRow(
    item: WatchHistoryItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val time = remember(item.watchedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.watchedAt))
    }
    val progress = item.progressFraction().coerceIn(0.02f, 1f)
    val watched = formatMs(item.positionMs)
    val total = if (item.durationMs > 0) formatMs(item.durationMs) else "--:--"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .width(24.dp)
                .height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(WuColors.SurfaceAlt),
            )
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) WuColors.AccentBlue else WuColors.Muted),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    time,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(WuColors.SurfaceAlt)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Text(
                    item.episode?.let { "Episode $it" } ?: "Episode",
                    color = WuColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text("$watched / $total", color = WuColors.Muted, fontSize = 11.sp)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WuColors.Progress,
                trackColor = WuColors.SurfaceAlt,
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = (ms / 1000L).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

private fun groupByDayLabel(items: List<WatchHistoryItem>): List<Pair<String, List<WatchHistoryItem>>> {
    val cal = Calendar.getInstance()
    val today = cal.clone() as Calendar
    val yesterday = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val fmt = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    val map = linkedMapOf<String, MutableList<WatchHistoryItem>>()
    items.forEach { item ->
        val c = Calendar.getInstance().apply { timeInMillis = item.watchedAt }
        val label = when {
            sameDay(c, today) -> "Hari ini"
            sameDay(c, yesterday) -> "Kemarin"
            else -> fmt.format(Date(item.watchedAt))
        }
        map.getOrPut(label) { mutableListOf() }.add(item)
    }
    return map.toList()
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
