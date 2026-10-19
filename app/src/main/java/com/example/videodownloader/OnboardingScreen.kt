package com.example.videodownloader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Onboarding Data Models & Definitions
// ---------------------------------------------------------------------------

data class OnboardingPageData(
    val illustrationRes: Int,
    val titleRes: Int,
    val subtitleRes: Int,
    val buttonTextRes: Int
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = OnboardingGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager for swipeable pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val pageData = onboardingPages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(28.dp))

                    // Illustration Image
                    Image(
                        painter = painterResource(id = pageData.illustrationRes),
                        contentDescription = stringResource(pageData.titleRes),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .widthIn(max = 340.dp)
                            .aspectRatio(1f)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Title
                    Text(
                        text = stringResource(pageData.titleRes),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subtitle
                    Text(
                        text = stringResource(pageData.subtitleRes),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xDDFFFFFF),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.widthIn(max = 310.dp)
                    )
                }
            }

            // Page Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    PageDot(active = pagerState.currentPage == index)
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Action Button (Next or Get Started)
            val isLastPage = pagerState.currentPage == onboardingPages.size - 1
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                Button(
                    onClick = {
                        if (isLastPage) {
                            onGetStarted()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1D4ED8)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (isLastPage) R.string.btn_get_started else R.string.btn_next
                        ),
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
        pageIndex = 2,
        pageData = onboardingPages[2],
        onButtonClick = onGetStarted
    )
}

@Composable
private fun OnboardingSinglePageView(
    pageIndex: Int,
    pageData: OnboardingPageData,
    onButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = OnboardingGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Illustration
            Image(
                painter = painterResource(id = pageData.illustrationRes),
                contentDescription = stringResource(pageData.titleRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .widthIn(max = 340.dp)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = stringResource(pageData.titleRes),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp,
                modifier = Modifier.widthIn(max = 320.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle
            Text(
                text = stringResource(pageData.subtitleRes),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xDDFFFFFF),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.widthIn(max = 310.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Page Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { i ->
                    PageDot(active = (i == pageIndex))
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
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

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun OnboardingScreenTwoPreview() {
    OnboardingScreenTwo()
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun OnboardingScreenThreePreview() {
    OnboardingScreenThree()
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun OnboardingPagerPreview() {
    OnboardingScreen()
}
