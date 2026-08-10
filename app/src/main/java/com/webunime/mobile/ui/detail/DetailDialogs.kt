package com.webunime.mobile.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webunime.mobile.BuildConfig
import com.webunime.mobile.R
import com.webunime.mobile.data.user.UserProfile
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.delay

data class XpGainInfo(
    val gained: Int,
    val level: Int,
    val xp: Int,
    val need: Int,
)

@Composable
fun KeysEmptyDialog(
    onBuyPremium: () -> Unit,
    onWatchAd: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WuColors.Surface)
                    .clickable(enabled = false) {}
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(WuColors.AccentYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_key),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Kunci Habis",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kunci diperlukan untuk membuka Episode\nYuk isi kuncinya dulu",
                    color = WuColors.Muted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Unlimited", color = Color.White, fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_key),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable(onClick = onBuyPremium)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Beli Premium", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Cuma Rp. 12.000", color = WuColors.Muted, fontSize = 11.sp)
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+ 1", color = Color.White, fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_key),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WuColors.AccentYellow)
                                .clickable(onClick = onWatchAd)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Tonton Iklan", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Iklan singkat", color = WuColors.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun XpGainPopup(
    info: XpGainInfo,
    onDismiss: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = (info.xp.toFloat() / info.need.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "xpBar",
    )
    LaunchedEffect(info) {
        delay(2200)
        onDismiss()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(180)) + scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            ),
            exit = fadeOut() + scaleOut(),
        ) {
            Column(
                Modifier
                    .padding(horizontal = 36.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(WuColors.Surface)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WuColors.SurfaceAlt),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = WuColors.AccentBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "+${info.gained} EXP",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WuColors.SurfaceAlt)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Base", color = WuColors.Muted, fontSize = 13.sp)
                    Text("+${info.gained}xp", color = Color.White, fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Level ${info.level}", color = Color.White, fontSize = 12.sp)
                    val sisa = (info.need - info.xp).coerceAtLeast(0)
                    Text("Sisa ${sisa}xp lagi", color = WuColors.Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = WuColors.AccentBlue,
                    trackColor = WuColors.SurfaceAlt,
                )
            }
        }
    }
}

@Composable
fun PremiumBanner(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1A237E), Color(0xFF0D47A1), Color(0xFF1565C0)),
                ),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2962FF))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(16.dp),
            )
            Text("Premium", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("Harga Mulai Dari Rp. 12.000", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("No iklan nonton sepuasnya", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        }
    }
}

@Composable
fun FloatingKeysPill(
    keys: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(WuColors.SurfaceAlt.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_key),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
        Text(
            keys.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

fun xpInfoFrom(profile: UserProfile, gained: Int = BuildConfig.XP_PER_EPISODE): XpGainInfo =
    XpGainInfo(
        gained = gained,
        level = profile.level,
        xp = profile.xp,
        need = profile.xpToNextLevel(),
    )
