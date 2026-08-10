package com.webunime.mobile.ui.timeline

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.ui.theme.WuColors

@Composable
fun TimelineScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onOpenAccount: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as WebunimeApp
    val profile by app.userRepository.profileFlow.collectAsStateWithLifecycle(initialValue = null)
    val name = profile?.displayName
        ?: app.session.displayName.ifBlank { "Weeboonime User" }
    val initial = name.take(1).uppercase()
    val level = profile?.level ?: 1
    val keys = profile?.keys ?: 0
    val gems = profile?.gems ?: 0

    Box(
        Modifier
            .fillMaxSize()
            .background(WuColors.Bg)
            .padding(contentPadding),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WuColors.SurfaceAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Kunci $keys · Gem $gems",
                        color = WuColors.Muted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Lvl. $level",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WuColors.SurfaceAlt)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                IconButton(onClick = onOpenAccount) {
                    Icon(Icons.Default.Settings, contentDescription = "Akun", tint = Color.White)
                }
            }

            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Belum ada aktivitas teman\nKamu bisa menambahkan teman dari komentar orang lain di anime favoritmu\n\nKetuk ikon gear untuk ekonomi akun (kunci / iklan / Premium).",
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 36.dp),
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FloatingActionButton(
                onClick = { },
                containerColor = Color.White,
                contentColor = Color.Black,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.People, contentDescription = "Teman")
            }
            FloatingActionButton(
                onClick = { },
                containerColor = Color.White,
                contentColor = Color.Black,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat")
            }
        }
    }
}
