package com.example.videodownloader

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.ads.SplashBottomNativeAd
import com.example.videodownloader.firebase.RemoteConfigManager

// ---------------------------------------------------------------------------
// Splash Screen
// Auto-navigates after ~4.5 seconds (4 to 5 sec display time)
// Supports adaptive layouts for Portrait and Landscape orientations
// ---------------------------------------------------------------------------

@Composable
fun SplashScreen(onFinished: () -> Unit = {}) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // ── Entry animations & Ad display check ──────────────────────────────
    val config by RemoteConfigManager.config.collectAsState()
    var visible by remember { mutableStateOf(false) }
    var adLoaded by remember { mutableStateOf(false) }
    var adUnavailable by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }

    fun triggerFinished() {
        if (!hasFinished) {
            hasFinished = true
            onFinished()
        }
    }

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

    LaunchedEffect(Unit) {
        visible = true
    }

    // ── Ad Display Timer: When ad loads, keep it on screen for `splash_ad_display_ms` ──
    LaunchedEffect(adLoaded) {
        if (adLoaded) {
            delay(config.splashAdDisplayMs)
            triggerFinished()
        }
    }

    // ── No ad (disabled in Remote Config / failed to load): move on after `splash_no_ad_delay_ms` ──
    LaunchedEffect(adUnavailable) {
        if (adUnavailable) {
            delay(config.splashNoAdDelayMs)
            triggerFinished()
        }
    }

    // ── Fallback safety timer (`splash_timeout_ms`) in case of no internet or ad hang ──
    LaunchedEffect(Unit) {
        delay(config.splashTimeoutMs)
        triggerFinished()
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
        GlowBubble(
            size = 320.dp,
            color = Color(0x1AFFFFFF),
            modifier = Modifier.align(Alignment.TopCenter).offset(x = (-60).dp, y = 50.dp)
        )
        GlowBubble(
            size = 240.dp,
            color = Color(0x122979FF),
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 10.dp, y = (-60).dp)
        )
        GlowBubble(
            size = 180.dp,
            color = Color(0x10FFFFFF),
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = (-80).dp)
        )

        if (isLandscape) {
            // ── Landscape Layout: Side-by-side Row with scrollable safety ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .padding(bottom = 90.dp)
            ) {
                // ── Logo illustration ──
                Image(
                    painter = painterResource(id = R.drawable.welcome_screen_logo),
                    contentDescription = "Video Downloader Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(130.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                )

                Spacer(modifier = Modifier.width(32.dp))

                // ── Text & Loader Column ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Video Downloader",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(textAlpha)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Download videos from any platform\nquickly and easily",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xCCFFFFFF),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.alpha(textAlpha)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SplashDotsLoader(
                        dot1Scale = dot1Scale,
                        dot2Scale = dot2Scale,
                        dot3Scale = dot3Scale,
                        alpha = dotAlpha
                    )
                }
            }
        } else {
            // ── Portrait Layout: Center Branding Column ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 110.dp) // Clearance for full-width bottom native ad
            ) {
                Spacer(modifier = Modifier.weight(1f, fill = false).heightIn(min = 16.dp))

                // ── Logo illustration ──
                Image(
                    painter = painterResource(id = R.drawable.welcome_screen_logo),
                    contentDescription = "Video Downloader Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(150.dp)
                        .scale(logoScale)
                        .alpha(logoAlpha)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ── App name ──
                Text(
                    text = "Video Downloader",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Tagline ──
                Text(
                    text = "Download videos from any platform\nquickly and easily",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xCCFFFFFF),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Pulsing dot loader ──
                SplashDotsLoader(
                    dot1Scale = dot1Scale,
                    dot2Scale = dot2Scale,
                    dot3Scale = dot3Scale,
                    alpha = dotAlpha
                )

                Spacer(modifier = Modifier.weight(1f, fill = false).heightIn(min = 16.dp))
            }
        }

        // Placement 1: Full-Width Bottom Pinned Native Ad
        SplashBottomNativeAd(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            placement = AdManager.NATIVE_SPLASH,
            onAdLoaded = { adLoaded = true },
            onAdUnavailable = { adUnavailable = true }
        )
    }
}

// ---------------------------------------------------------------------------
// Pulsing dots loading indicator
// ---------------------------------------------------------------------------

@Composable
private fun SplashDotsLoader(
    dot1Scale: Float,
    dot2Scale: Float,
    dot3Scale: Float,
    alpha: Float
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.alpha(alpha)
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
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun SplashScreenPortraitPreview() {
    SplashScreen()
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=844dp,height=390dp")
@Composable
fun SplashScreenLandscapePreview() {
    SplashScreen()
}
