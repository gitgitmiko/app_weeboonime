package com.webunime.mobile.ui.premium

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.webunime.mobile.BuildConfig
import com.webunime.mobile.R
import com.webunime.mobile.WebunimeApp
import com.webunime.mobile.data.billing.PremiumPlan
import com.webunime.mobile.ui.theme.WuColors
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val FeatureList = listOf(
    "Badge Premium",
    "Unlimited Energy",
    "Menghilangkan Iklan",
    "Komentar lebih dari 1x",
    "Kualitas 1080P",
    "Mini Videoplayer",
    "Foto Profil Bergerak",
    "Bonus EXP Level 50%",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PremiumPackageScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val activity = context as Activity
    val app = context.applicationContext as WebunimeApp
    val scope = rememberCoroutineScope()
    val profile by app.userRepository.profileFlow.collectAsStateWithLifecycle(initialValue = null)

    val name = profile?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: app.session.displayName.takeIf { it.isNotBlank() }
        ?: "Weeboonime User"
    val tag = profile?.publicTag() ?: "#GUEST"
    val isPremium = profile?.effectivePremium() == true

    Column(
        Modifier
            .fillMaxSize()
            .background(WuColors.Bg)
            .padding(contentPadding),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A2744), WuColors.Bg),
                        ),
                    ),
            )
            Column {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White,
                    )
                }
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "Weeboonime Premium",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Pilih paket premium kamu, tenang ini bukan berlangganan",
                        color = WuColors.Muted,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(WuColors.Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WuColors.SurfaceAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    val photo = profile?.photoUrl
                    if (!photo.isNullOrBlank()) {
                        AsyncImage(
                            model = photo,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(
                        "$name$tag",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        if (isPremium) "Premium aktif" else "Belum Premium",
                        color = WuColors.Muted,
                        fontSize = 13.sp,
                    )
                }
            }

            Text(
                "Pilih Paket",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )

            app.billingRepository.plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    onClick = {
                        val launched = app.billingRepository.launchPlan(activity, plan.productId)
                        if (!launched) {
                            if (BuildConfig.DEBUG) {
                                scope.launch {
                                    app.userRepository.applyPremiumDays(plan.days, plan.bonusGems)
                                    Toast.makeText(
                                        context,
                                        "Debug: ${plan.title} aktif (+${formatGems(plan.bonusGems)} gem)",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Billing belum siap / produk belum di Play Console",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WuColors.Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Fitur Premium",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureList.forEach { feature ->
                        Text(
                            feature,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(WuColors.SurfaceAlt)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Text(
                    "1x pembayaran, bukan perpanjangan otomatis. No refund.",
                    color = Color(0xFFD4A84B),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF8B6914), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (plan.isBest) Color(0xFF5AB5FF) else Color.Transparent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WuColors.Surface)
            .then(
                if (plan.isBest) Modifier.border(1.5.dp, borderColor, shape)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                if (plan.isBest) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Best",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2F80FF))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Text(plan.subtitle, color = WuColors.Muted, fontSize = 12.sp)
            Row(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2F80FF).copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gem),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "+${formatGems(plan.bonusGems)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                plan.priceLabel,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            plan.savePercent?.let { pct ->
                Text(
                    "Hemat $pct%",
                    color = Color(0xFF4CAF50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun formatGems(n: Int): String =
    NumberFormat.getIntegerInstance(Locale("id", "ID")).format(n)
