package com.webunime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.webunime.mobile.ui.theme.WuColors

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = WuColors.Link,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
fun WibukuPosterCard(
    title: String,
    thumbnail: String?,
    episodeLabel: String? = null,
    rating: String? = null,
    viewsLabel: String? = null,
    showNew: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            AsyncImage(
                model = thumbnail,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.55f to Color.Transparent,
                            1f to Color(0xCC000000),
                        ),
                    ),
            )
            if (showNew) {
                Text(
                    text = "New",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WuColors.NewBadge)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!rating.isNullOrBlank()) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        Icons.Outlined.Star,
                        null,
                        tint = WuColors.AccentYellow,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(rating, color = Color.White, fontSize = 10.sp)
                }
            }
            if (!episodeLabel.isNullOrBlank()) {
                Text(
                    text = episodeLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                )
            }
        }
        if (!viewsLabel.isNullOrBlank()) {
            Row(
                Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.RemoveRedEye,
                    null,
                    tint = WuColors.Muted,
                    modifier = Modifier.size(12.dp),
                )
                Text(viewsLabel, color = WuColors.Muted, fontSize = 11.sp, maxLines = 1)
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
        )
    }
}

@Composable
fun <T> HorizontalWibukuPosterRow(
    items: List<T>,
    titleOf: (T) -> String,
    thumbOf: (T) -> String?,
    episodeOf: (T) -> String? = { null },
    ratingOf: (T) -> String? = { null },
    viewsOf: (T) -> String? = { null },
    showNewOf: (T) -> Boolean = { false },
    onClick: (T) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items) { item ->
            WibukuPosterCard(
                title = titleOf(item),
                thumbnail = thumbOf(item),
                episodeLabel = episodeOf(item),
                rating = ratingOf(item),
                viewsLabel = viewsOf(item),
                showNew = showNewOf(item),
                onClick = { onClick(item) },
                modifier = Modifier.width(118.dp),
            )
        }
    }
}

/** Kompatibilitas layar lama */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    SectionHeader(title = title)
}

@Composable
fun PosterCard(
    title: String,
    thumbnail: String?,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WibukuPosterCard(
        title = title,
        thumbnail = thumbnail,
        episodeLabel = subtitle,
        onClick = onClick,
        modifier = modifier.width(118.dp),
    )
}

@Composable
fun <T> HorizontalPosterRow(
    items: List<T>,
    titleOf: (T) -> String,
    thumbOf: (T) -> String?,
    subtitleOf: (T) -> String? = { null },
    onClick: (T) -> Unit,
) {
    HorizontalWibukuPosterRow(
        items = items,
        titleOf = titleOf,
        thumbOf = thumbOf,
        episodeOf = subtitleOf,
        onClick = onClick,
    )
}
