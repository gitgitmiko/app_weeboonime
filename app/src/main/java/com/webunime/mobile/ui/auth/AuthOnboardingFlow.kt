package com.webunime.mobile.ui.auth

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webunime.mobile.BuildConfig
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val AuthBg = Color(0xFF121212)
private val AuthMuted = Color(0xFFA0A0A1)
private val ButtonBg = Color(0xFF2B2C2F)
private val LinkCyan = Color(0xFF5AB5FF)
private val BrandAccent = Color(0xFFFFC107)

@Composable
fun AuthOnboardingFlow(
    onGoogleLogin: () -> Unit,
    onTesterLogin: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    fun goNext() {
        scope.launch {
            val next = (pagerState.currentPage + 1).coerceAtMost(2)
            pagerState.animateScrollToPage(next)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(AuthBg),
        beyondViewportPageCount = 1,
    ) { page ->
        when (page) {
            0 -> WelcomePage(onSwipeHint = { goNext() }, pagerState = pagerState)
            1 -> DisclaimerPage(onSwipeHint = { goNext() }, pagerState = pagerState)
            else -> LoginPage(
                onGoogleLogin = onGoogleLogin,
                onTesterLogin = onTesterLogin,
                onOpenPrivacy = onOpenPrivacy,
                onOpenTerms = onOpenTerms,
            )
        }
    }
}

@Composable
private fun AuthBrandHeader() {
    Column(
        Modifier
            .statusBarsPadding()
            .padding(top = 20.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "WEEBOONIME",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "◆",
                color = BrandAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            color = AuthMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SwipeUpHint() {
    val bob by rememberInfiniteTransition(label = "swipe").animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val dot by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.2f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "dotY",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset { IntOffset(0, bob.roundToInt()) },
    ) {
        Canvas(Modifier.size(width = 18.dp, height = 28.dp)) {
            drawRoundRect(
                color = Color.White,
                style = Stroke(width = 2.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(
                    x = size.width / 2f,
                    y = size.height * dot,
                ),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("Swipe Up to continue", color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun WelcomePage(
    onSwipeHint: () -> Unit,
    pagerState: PagerState,
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = pagerState.currentPage == 0, onClick = onSwipeHint),
    ) {
        AuthBrandHeader()
        Text(
            text = ".WELCOME.",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
        ) { SwipeUpHint() }
    }
}

@Composable
private fun DisclaimerPage(
    onSwipeHint: () -> Unit,
    pagerState: PagerState,
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = pagerState.currentPage == 1, onClick = onSwipeHint),
    ) {
        AuthBrandHeader()
        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
        ) {
            Text(
                "Disclaimer",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            val body = buildAnnotatedString {
                append(
                    "Weeboonime adalah aplikasi tidak resmi untuk menelusuri katalog anime. " +
                        "Semua merek dagang dan hak cipta milik pemiliknya masing-masing. " +
                        "Kami tidak menyimpan file video di server kami. " +
                        "Masalah hak cipta dapat dikirim ke ",
                )
                withStyle(SpanStyle(color = LinkCyan, textDecoration = TextDecoration.Underline)) {
                    append("admin@weeboonime.app")
                }
                append(". Dengan menggunakan aplikasi ini, kamu menyetujui kebijakan kami.")
            }
            Text(
                text = body,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
        ) { SwipeUpHint() }
    }
}

@Composable
private fun LoginPage(
    onGoogleLogin: () -> Unit,
    onTesterLogin: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AuthBrandHeader()

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Login",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Aplikasi kami mewajibkan kamu untuk login menggunakan email Google",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(ButtonBg)
                    .clickable(onClick = onGoogleLogin)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "G",
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Tekan di sini untuk Login",
                    color = Color.White,
                    fontSize = 16.sp,
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 28.dp, vertical = 36.dp)
                .fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E2A3A))
                    .clickable(onClick = onTesterLogin)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.Icon(
                    Icons.Outlined.Dialpad,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text("Tester Login", color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "Dengan melakukan login maka kamu telah setuju dengan",
                color = Color.White,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    text = "Privacy Policy",
                    color = LinkCyan,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onOpenPrivacy),
                )
                Text(text = " & ", color = Color.White, fontSize = 13.sp)
                Text(
                    text = "Terms of Service",
                    color = LinkCyan,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onOpenTerms),
                )
            }
        }
    }
}
