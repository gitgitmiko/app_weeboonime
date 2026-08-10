package com.webunime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webunime.mobile.ui.theme.WuColors

/** Banner promo Premium di beranda (layout ala Wibuku). */
@Composable
fun HomePremiumPromo(
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier,
    startingPrice: String = "Rp. 12.000",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(WuColors.Surface)
            .clickable(onClick = onOpenPremium)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerPremiumButton(onClick = onOpenPremium)
        Column(Modifier.weight(1f)) {
            Text(
                "Harga Mulai Dari $startingPrice",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "No iklan nonton sepuasnya",
                color = WuColors.Muted,
                fontSize = 11.sp,
            )
        }
    }
}
