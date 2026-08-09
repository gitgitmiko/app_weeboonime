package com.webunime.mobile.ui.auth

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webunime.mobile.BuildConfig

private val AuthMuted = Color(0xFFA0A0A1)
private val ButtonBg = Color(0xFF2B2C2F)
private val LinkCyan = Color(0xFF5AB5FF)

@Composable
fun LoginScreen(
    onGoogleLogin: () -> Unit,
    onTesterLogin: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "WEEBOONIME",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = "App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = AuthMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

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
                    .background(ButtonBg)
                    .clickable(onClick = onTesterLogin)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
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
