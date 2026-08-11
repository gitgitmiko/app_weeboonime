package com.webunime.mobile.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.compose.AsyncImage
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.CalendarResponse
import com.webunime.mobile.data.ScheduleItem
import com.webunime.mobile.data.toEpisodeLabel
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Instant

@Composable
fun CalendarScreen(
    onOpenAnime: (slug: String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    val scope = rememberCoroutineScope()
    var cal by remember { mutableStateOf<CalendarResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableIntStateOf(0) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { app.catalogApi.calendar() }
                .onSuccess { data ->
                    cal = data
                    val idx = data.days.indexOfFirst { it.day == data.today }
                    selected = if (idx >= 0) idx else 0
                }
                .onFailure { error = it.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    when {
        loading && cal == null -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(color = WuColors.AccentBlue) }

        error != null && cal == null -> Box(
            Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error ?: "", color = WuColors.Muted)
                TextButton(onClick = { reload() }) { Text("Coba lagi") }
            }
        }

        else -> {
            val data = cal ?: return
            val day = data.days.getOrNull(selected)
            val weekDays = remember(data.days.size) { weekDaysJakarta(data.days.size) }
            val selectedDate = weekDays.getOrNull(selected)?.date
            val prevLabel = data.days.getOrNull(selected - 1)?.shortLabel()
            val nextLabel = data.days.getOrNull(selected + 1)?.shortLabel()

            Box(
                Modifier
                    .fillMaxSize()
                    .background(WuColors.Bg)
                    .padding(contentPadding),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        text = "Jadwal Tayang",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 14.dp, bottom = 12.dp),
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(data.days) { index, d ->
                            val active = index == selected
                            val isToday = d.day == data.today
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) WuColors.SurfaceAlt else Color.Transparent)
                                    .clickable { selected = index }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = d.shortLabel(),
                                    color = if (active) Color.White else WuColors.Muted,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (active) WuColors.AccentBlue else Color.Transparent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = weekDays.getOrNull(index)?.dayOfMonth?.toString()
                                            ?: (index + 1).toString(),
                                        color = if (active) Color.White else WuColors.Muted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) WuColors.AccentBlue else Color.Transparent),
                                )
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(day?.items.orEmpty()) { item ->
                            ScheduleRow(
                                item = item,
                                dayDate = selectedDate,
                                onClick = { item.slug?.let(onOpenAnime) },
                            )
                        }
                    }
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (prevLabel != null) {
                        DayNavPill(
                            label = prevLabel,
                            leading = true,
                            onClick = { selected = (selected - 1).coerceAtLeast(0) },
                        )
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (nextLabel != null) {
                        DayNavPill(
                            label = nextLabel,
                            leading = false,
                            onClick = { selected = (selected + 1).coerceAtMost(data.days.lastIndex) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNavPill(label: String, leading: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .border(1.dp, WuColors.SurfaceAlt, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        if (!leading) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

private enum class AirKind { AIRED, UPCOMING, LATE }

private data class AirStatus(
    val kind: AirKind,
    val episode: Double,
    val label: String,
)

@Composable
private fun ScheduleRow(
    item: ScheduleItem,
    dayDate: LocalDate?,
    onClick: () -> Unit,
) {
    val status = remember(item, dayDate) { resolveAirStatus(item, dayDate) }
    val statusColor = when (status.kind) {
        AirKind.AIRED -> WuColors.AccentYellow
        AirKind.UPCOMING -> WuColors.Muted
        AirKind.LATE -> WuColors.AccentRed
    }
    val barColor = when (status.kind) {
        AirKind.AIRED -> WuColors.AccentYellow
        AirKind.UPCOMING -> WuColors.SurfaceAlt
        AirKind.LATE -> WuColors.AccentRed
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WuColors.Surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = item.time?.takeIf { it.isNotBlank() } ?: "--:--",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.width(48.dp),
        )
        AsyncImage(
            model = item.thumbnail,
            contentDescription = item.displayTitle(),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp, 74.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.displayTitle(),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Eps ${status.episode.toEpisodeLabel()}",
                color = WuColors.Muted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!item.rating.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Outlined.Star, null, tint = WuColors.AccentYellow, modifier = Modifier.size(13.dp))
                        Text(item.rating, color = WuColors.Muted, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    status.label,
                    color = statusColor,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private fun com.webunime.mobile.data.ScheduleDay.shortLabel(): String {
    val raw = (label ?: day).orEmpty()
    return when {
        raw.startsWith("Min", true) || raw.equals("Sunday", true) -> "Min"
        raw.startsWith("Sen", true) || raw.equals("Monday", true) -> "Sen"
        raw.startsWith("Sel", true) || raw.equals("Tuesday", true) -> "Sel"
        raw.startsWith("Rab", true) || raw.equals("Wednesday", true) -> "Rab"
        raw.startsWith("Kam", true) || raw.equals("Thursday", true) -> "Kam"
        raw.startsWith("Jum", true) || raw.equals("Friday", true) -> "Jum"
        raw.startsWith("Sab", true) || raw.equals("Saturday", true) -> "Sab"
        else -> raw.take(3).ifBlank { "?" }
    }
}

private data class WeekDayInfo(val dayOfMonth: Int, val date: LocalDate)

/** Senin–Minggu minggu berjalan (Asia/Jakarta), selaras urutan API. */
private fun weekDaysJakarta(count: Int): List<WeekDayInfo> {
    val zone = ZoneId.of("Asia/Jakarta")
    val today = LocalDate.now(zone)
    val monday = today.with(DayOfWeek.MONDAY)
    return (0 until count).map { i ->
        val d = monday.plusDays(i.toLong())
        WeekDayInfo(dayOfMonth = d.dayOfMonth, date = d)
    }
}

private fun resolveAirStatus(item: ScheduleItem, dayDate: LocalDate?): AirStatus {
    val zone = ZoneId.of("Asia/Jakarta")
    val now = ZonedDateTime.now(zone)
    val latest = item.latest_episode?.takeIf { it > 0 } ?: 0.0
    val nextEp = latest + 1
    val date = dayDate ?: now.toLocalDate()
    val airAt = parseAirAt(date, item.time, zone)
    val releasedOk = releaseCoversAirSlot(
        releasedAtRaw = item.latest_released_at,
        dayDate = date,
        airAt = airAt,
        zone = zone,
    )

    if (latest > 0 && releasedOk) {
        return AirStatus(AirKind.AIRED, latest, "Sudah Tayang")
    }
    if (now.isBefore(airAt)) {
        return AirStatus(AirKind.UPCOMING, nextEp, "Belum Tayang")
    }
    return AirStatus(AirKind.LATE, nextEp, "Belum Tayang (Terlambat)")
}

private fun parseAirAt(
    date: LocalDate,
    timeRaw: String?,
    zone: ZoneId,
): ZonedDateTime {
    val time = timeRaw?.trim().orEmpty()
    if (time.isEmpty()) return date.atTime(23, 59).atZone(zone)
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 23
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 59
    return date.atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).atZone(zone)
}

private fun releaseCoversAirSlot(
    releasedAtRaw: String?,
    dayDate: LocalDate,
    airAt: ZonedDateTime,
    zone: ZoneId,
): Boolean {
    val released = parseReleasedAt(releasedAtRaw, zone) ?: return false
    if (released.toLocalDate() == dayDate) return true
    if (!released.isBefore(airAt.minusHours(12))) return true
    return false
}

private fun parseReleasedAt(raw: String?, zone: ZoneId): ZonedDateTime? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim()
    runCatching { return Instant.parse(s).atZone(zone) }
    runCatching { return LocalDateTime.parse(s).atZone(zone) }
    runCatching { return LocalDate.parse(s.take(10)).atStartOfDay(zone) }
    return null
}
