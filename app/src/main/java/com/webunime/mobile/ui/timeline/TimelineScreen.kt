package com.webunime.mobile.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private data class DummyTimeline(
    val user: String,
    val action: String,
    val time: String,
)

private val dummyFeed = listOf(
    DummyTimeline("WeeFan_01", "baru saja menonton Solo Leveling Ep 8", "2 jam lalu"),
    DummyTimeline("MikoOtaku", "subscribe Frieren", "5 jam lalu"),
    DummyTimeline("NekoNight", "selesai binge Dandadan", "Kemarin"),
    DummyTimeline("AoiWatch", "menambahkan Kaiju No. 8 ke list", "Kemarin"),
    DummyTimeline("RikuStream", "beri rating 9/10 untuk Bocchi the Rock", "2 hari lalu"),
)

/**
 * Tab Timeline (UI ala Wibuku / sosial).
 * Belum ada backend — tampilan dummy.
 */
@Composable
fun TimelineScreen(
    contentPadding: PaddingValues = PaddingValues(),
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Text(
            text = "Timeline",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            text = "Aktivitas komunitas segera hadir. Contoh feed di bawah.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(dummyFeed) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1F22))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2C2F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.user.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.user,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            item.action,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(
                            item.time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}
