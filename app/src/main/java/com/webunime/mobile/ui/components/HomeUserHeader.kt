package com.webunime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webunime.mobile.R
import com.webunime.mobile.data.user.UserProfile
import com.webunime.mobile.ui.theme.WuColors

@Composable
fun HomeUserHeader(
    profile: UserProfile?,
    sessionName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = profile?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: sessionName.takeIf { it.isNotBlank() }
        ?: "Weeboonime User"
    val tag = profile?.publicTag() ?: "#GUEST"
    val level = profile?.level ?: 1
    val xp = profile?.xp ?: 0
    val need = profile?.xpToNextLevel() ?: 50
    val progress = profile?.xpProgress() ?: 0f
    val keys = profile?.keys ?: 0
    val gems = profile?.gems ?: 0
    val photo = profile?.photoUrl
    val premium = profile?.effectivePremium() == true

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WuColors.Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(WuColors.SurfaceAlt),
                contentAlignment = Alignment.Center,
            ) {
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = WuColors.Muted,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tag,
                    color = WuColors.Link,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (premium) {
                    Text(
                        text = "Premium",
                        color = WuColors.AccentYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EconomyChip(iconRes = R.drawable.ic_key, value = keys.toString(), tint = WuColors.AccentYellow)
                EconomyChip(iconRes = R.drawable.ic_gem, value = gems.toString(), tint = WuColors.AccentBlue)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_xp),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Lvl. $level",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "$xp / $need XP",
                    color = WuColors.Muted,
                    fontSize = 12.sp,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = WuColors.AccentBlue,
                trackColor = WuColors.SurfaceAlt,
            )
        }
    }
}

@Composable
fun EconomyChip(
    iconRes: Int,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(WuColors.SurfaceAlt)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = value,
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}
