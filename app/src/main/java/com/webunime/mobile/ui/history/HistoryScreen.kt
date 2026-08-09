package com.webunime.mobile.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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

@Composable
fun HistoryScreen(
    onOpenAnime: (slug: String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    var items by remember { mutableStateOf(app.watchHistory.list()) }
    var multi by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) items = app.watchHistory.list()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val grouped = remember(items) { groupByDayLabel(items) }

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
                text = "Tap tahan untuk memilih & hapus",
                color = WuColors.Muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 8.dp),
            )

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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    grouped.forEach { (day, dayItems) ->
                        item {
                            Text(
                                text = day,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 8.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(WuColors.AccentBlue)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        items(dayItems, key = { "${it.slug}-${it.watchedAt}" }) { item ->
                            HistoryCard(
                                item = item,
                                multi = multi,
                                onClick = { onOpenAnime(item.slug) },
                            )
                        }
                    }
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

@Composable
private fun HistoryCard(
    item: WatchHistoryItem,
    multi: Boolean,
    onClick: () -> Unit,
) {
    val time = remember(item.watchedAt) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.watchedAt))
    }
    // Dummy progress visual ala Wibuku (belum ada posisi player nyata)
    val progress = remember(item.slug, item.episode) {
        (((item.episode ?: 1) * 17) % 85 + 10) / 100f
    }
    val watched = formatDuration((progress * 23 * 60).toInt())
    val total = "23:55"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(72.dp)
                    .background(WuColors.SurfaceAlt),
            )
            Text(
                text = time,
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(WuColors.SurfaceAlt)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        AsyncImage(
            model = item.thumbnail,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(if (multi) 48.dp else 56.dp)
                .clip(if (multi) CircleShape else RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
                    .padding(top = 8.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = WuColors.Progress,
                trackColor = WuColors.SurfaceAlt,
            )
            Text(
                "$watched / $total",
                color = WuColors.Muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun formatDuration(totalSec: Int): String {
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
