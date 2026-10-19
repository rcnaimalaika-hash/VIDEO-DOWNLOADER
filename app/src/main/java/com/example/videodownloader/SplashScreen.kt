package com.example.videodownloader

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Splash Screen
// Auto-navigates after ~2.5 seconds
// ---------------------------------------------------------------------------

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {

    // ── Entry animations ──────────────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.6f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label         = "logoAlpha"
    )
    val textAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 400, easing = FastOutSlowInEasing),
        label         = "textAlpha"
    )
    val dotAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 700, easing = FastOutSlowInEasing),
        label         = "dotAlpha"
    )

    // ── Pulsing dots loading indicator ────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "d1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 150, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "d2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue  = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(500, delayMillis = 300, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "d3"
    )

    // ── Timer ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        visible = true
        delay(2500)
        onFinished()
    }

    // ── UI ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A6BFF),
                        Color(0xFF1050D8),
                        Color(0xFF0A35A8)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // Background glow bubbles
        GlowBubble(size = 320.dp, color = Color(0x1AFFFFFF),
            modifier = Modifier.align(Alignment.TopCenter).offset(x = (-60).dp, y = 50.dp))
        GlowBubble(size = 240.dp, color = Color(0x122979FF),
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 10.dp, y = (-60).dp))
        GlowBubble(size = 180.dp, color = Color(0x10FFFFFF),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-80).dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // ── Logo illustration ─────────────────────────────────────────
            Image(
                painter         = painterResource(id = R.drawable.welcome_screen_logo),
                contentDescription = "Video Downloader Logo",
                contentScale    = ContentScale.Fit,
                modifier        = Modifier
                    .size(280.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── App name ──────────────────────────────────────────────────
            Text(
                text       = "Video Downloader",
                fontSize   = 30.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Tagline ───────────────────────────────────────────────────
            Text(
                text       = "Download videos from any platform\nquickly and easily",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Normal,
                color      = Color(0xCCFFFFFF),
                textAlign  = TextAlign.Center,
                lineHeight = 22.sp,
                modifier   = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── Pulsing dot loader ────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier.alpha(dotAlpha)
            ) {
                listOf(dot1Scale, dot2Scale, dot3Scale).forEachIndexed { i, scale ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                if (i == 0) Color.White else Color(0x88FFFFFF)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Glow bubble helper
// ---------------------------------------------------------------------------

@Composable
private fun GlowBubble(size: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .blur(60.dp)
            .background(color, CircleShape)
    )
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}
