package com.example.videodownloader

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.ads.FullScreenNativeAdCard
import com.example.videodownloader.firebase.AnalyticsManager
import com.example.videodownloader.firebase.RemoteConfigManager

// ---------------------------------------------------------------------------
// Onboarding Data Models & Definitions
// ---------------------------------------------------------------------------

data class OnboardingPageData(
    val illustrationRes: Int = R.drawable.onboarding_illustration_1,
    val titleRes: Int = R.string.onboarding_1_title,
    val subtitleRes: Int = R.string.onboarding_1_subtitle,
    val buttonTextRes: Int = R.string.btn_next,
    val isNativeAd: Boolean = false
)

val onboardingPages = listOf(
    OnboardingPageData(
        illustrationRes = R.drawable.onboarding_illustration_1,
        titleRes = R.string.onboarding_1_title,
        subtitleRes = R.string.onboarding_1_subtitle,
        buttonTextRes = R.string.btn_next
    ),
    OnboardingPageData(
        illustrationRes = R.drawable.onboarding_illustration_2,
        titleRes = R.string.onboarding_2_title,
        subtitleRes = R.string.onboarding_2_subtitle,
        buttonTextRes = R.string.btn_next
    ),
    OnboardingPageData(
        illustrationRes = R.drawable.onboarding_illustration_2,
        titleRes = R.string.onboarding_2_title,
        subtitleRes = R.string.onboarding_2_subtitle,
        buttonTextRes = R.string.btn_next,
        isNativeAd = true
    ),
    OnboardingPageData(
        illustrationRes = R.drawable.onboarding_illustration_3,
        titleRes = R.string.onboarding_3_title,
        subtitleRes = R.string.onboarding_3_subtitle,
        buttonTextRes = R.string.btn_get_started
    )
)

private val OnboardingGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color(0xFF135DE9),
        0.5f to Color(0xFF0241C2),
        0.9f to Color(0xFF042E9A),
        1f to Color(0xFF042E9A)
    )
)

// ---------------------------------------------------------------------------
// Main Interactive Onboarding Screen (HorizontalPager)
// ---------------------------------------------------------------------------

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Remote Config: drop the sponsored page entirely when `ad_onboarding_native_enabled`
    // (or the master switches) are off. Frozen for this session so the pager stays stable.
    val pages = remember {
        val cfg = RemoteConfigManager.current
        if (cfg.isNativeEnabled(AdManager.NATIVE_ONBOARDING)) onboardingPages
        else onboardingPages.filter { !it.isNativeAd }
    }
    val adCountdownSec = remember { RemoteConfigManager.current.onboardingAdCountdownSec.toInt().coerceIn(0, 30) }

    LaunchedEffect(Unit) { AnalyticsManager.logTutorialBegin() }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    var nativeAdLoaded by remember { mutableStateOf(false) }
    var adCountdown by remember { mutableIntStateOf(adCountdownSec) }

    val isCurrentPageNativeAd = pages.getOrNull(pagerState.currentPage)?.isNativeAd == true
    val isAdLocked = isCurrentPageNativeAd && adCountdown > 0

    // ── Active Native Ad Display Timer (5 seconds) ──
    LaunchedEffect(pagerState.currentPage, nativeAdLoaded) {
        if (isCurrentPageNativeAd && nativeAdLoaded) {
            adCountdown = adCountdownSec
            while (adCountdown > 0) {
                delay(1000)
                adCountdown -= 1
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = OnboardingGradient)
    ) {
        if (isLandscape) {
            // ── Landscape: Horizontal arrangement with scrollable content ──
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pager takes main left portion
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isAdLocked,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) { pageIndex ->
                    val pageData = pages[pageIndex]
                    if (pageData.isNativeAd) {
                        FullScreenNativeAdCard(
                            onContinue = {
                                if (!isAdLocked) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pageIndex + 1)
                                    }
                                }
                            },
                            onAdLoaded = { nativeAdLoaded = true }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = pageData.illustrationRes),
                                contentDescription = stringResource(pageData.titleRes),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(140.dp)
                            )

                            Spacer(modifier = Modifier.width(24.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(pageData.titleRes),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(pageData.subtitleRes),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xDDFFFFFF),
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right controls: Page dots + Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pages.size) { index ->
                            PageDot(active = pagerState.currentPage == index)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (isLastPage) {
                                AnalyticsManager.logTutorialComplete()
                                onGetStarted()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = !isAdLocked,
                        modifier = Modifier
                            .height(50.dp)
                            .widthIn(min = 140.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1D4ED8),
                            disabledContainerColor = Color(0x66FFFFFF),
                            disabledContentColor = Color(0x991D4ED8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isAdLocked) {
                                "Next (${adCountdown}s)"
                            } else if (isLastPage) {
                                stringResource(R.string.btn_get_started)
                            } else {
                                stringResource(R.string.btn_next)
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdLocked) Color(0x991D4ED8) else Color(0xFF1D4ED8)
                        )
                    }
                }
            }
        } else {
            // ── Portrait: Vertical arrangement with responsive sizes and scroll safety ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isAdLocked,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val pageData = pages[pageIndex]
                    if (pageData.isNativeAd) {
                        FullScreenNativeAdCard(
                            onContinue = {
                                if (!isAdLocked) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pageIndex + 1)
                                    }
                                }
                            },
                            onAdLoaded = { nativeAdLoaded = true }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Image(
                                painter = painterResource(id = pageData.illustrationRes),
                                contentDescription = stringResource(pageData.titleRes),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
                                    .fillMaxWidth(0.75f)
                                    .aspectRatio(1f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(pageData.titleRes),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp,
                                modifier = Modifier.widthIn(max = 320.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(pageData.subtitleRes),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xDDFFFFFF),
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp,
                                modifier = Modifier.widthIn(max = 310.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        PageDot(active = pagerState.currentPage == index)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Button(
                        onClick = {
                            if (isLastPage) {
                                AnalyticsManager.logTutorialComplete()
                                onGetStarted()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = !isAdLocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1D4ED8),
                            disabledContainerColor = Color(0x66FFFFFF),
                            disabledContentColor = Color(0x991D4ED8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = if (isAdLocked) {
                                "Next (${adCountdown}s)"
                            } else if (isLastPage) {
                                stringResource(R.string.btn_get_started)
                            } else {
                                stringResource(R.string.btn_next)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdLocked) Color(0x991D4ED8) else Color(0xFF1D4ED8),
                            lineHeight = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Standalone Screen Composables
// ---------------------------------------------------------------------------

@Composable
fun OnboardingScreenOne(onNext: () -> Unit = {}) {
    OnboardingSinglePageView(
        pageIndex = 0,
        pageData = onboardingPages[0],
        onButtonClick = onNext
    )
}

@Composable
fun OnboardingScreenTwo(onNext: () -> Unit = {}) {
    OnboardingSinglePageView(
        pageIndex = 1,
        pageData = onboardingPages[1],
        onButtonClick = onNext
    )
}

@Composable
fun OnboardingScreenThree(onGetStarted: () -> Unit = {}) {
    OnboardingSinglePageView(
        pageIndex = 3,
        pageData = onboardingPages.last(),
        onButtonClick = onGetStarted
    )
}

@Composable
private fun OnboardingSinglePageView(
    pageIndex: Int,
    pageData: OnboardingPageData,
    onButtonClick: () -> Unit
) {
    if (pageData.isNativeAd) {
        FullScreenNativeAdCard(onContinue = onButtonClick)
        return
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = OnboardingGradient)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = pageData.illustrationRes),
                        contentDescription = stringResource(pageData.titleRes),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(140.dp)
                    )

                    Spacer(modifier = Modifier.width(24.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(pageData.titleRes),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(pageData.subtitleRes),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xDDFFFFFF),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { i ->
                            PageDot(active = (i == pageIndex))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onButtonClick,
                        modifier = Modifier
                            .height(50.dp)
                            .widthIn(min = 140.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1D4ED8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(pageData.buttonTextRes),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(id = pageData.illustrationRes),
                        contentDescription = stringResource(pageData.titleRes),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .sizeIn(maxWidth = 280.dp, maxHeight = 280.dp)
                            .fillMaxWidth(0.75f)
                            .aspectRatio(1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(pageData.titleRes),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(pageData.subtitleRes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xDDFFFFFF),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.widthIn(max = 310.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { i ->
                        PageDot(active = (i == pageIndex))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Button(
                        onClick = onButtonClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1D4ED8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = stringResource(pageData.buttonTextRes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            lineHeight = 28.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Page Indicator Dot
// ---------------------------------------------------------------------------

@Composable
private fun PageDot(active: Boolean) {
    val size by animateDpAsState(
        targetValue = if (active) 10.dp else 8.dp,
        animationSpec = tween(durationMillis = 300),
        label = "dotSize"
    )
    val color by animateColorAsState(
        targetValue = if (active) Color.White else Color(0x66FFFFFF),
        animationSpec = tween(durationMillis = 300),
        label = "dotColor"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun OnboardingScreenOnePreview() {
    OnboardingScreenOne()
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=844dp,height=390dp")
@Composable
fun OnboardingLandscapePreview() {
    OnboardingScreen()
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun OnboardingPagerPreview() {
    OnboardingScreen()
}

