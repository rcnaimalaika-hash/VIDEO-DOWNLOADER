package com.example.videodownloader.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.videodownloader.firebase.RemoteConfigManager
import com.example.videodownloader.ui.theme.AppColors
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import java.util.concurrent.atomic.AtomicBoolean

/**
 * State holder for a Native Ad load request in Jetpack Compose.
 *
 * Waits (briefly) for the first Remote Config fetch so a remote "off" switch
 * is honoured even on the very first launch, then loads the ad for
 * [placement] using the ad-unit id configured for it. [onAdUnavailable] fires
 * when the placement is disabled or the load fails, so callers can collapse
 * the slot / continue their flow.
 */
@Composable
fun rememberNativeAdState(
    placement: String,
    onAdLoaded: () -> Unit = {},
    onAdUnavailable: () -> Unit = {}
): State<NativeAd?> {
    val context = LocalContext.current
    val nativeAdState = remember { mutableStateOf<NativeAd?>(null) }
    val latestOnLoaded by rememberUpdatedState(onAdLoaded)
    val latestOnUnavailable by rememberUpdatedState(onAdUnavailable)
    val disposed = remember { AtomicBoolean(false) }

    LaunchedEffect(placement) {
        RemoteConfigManager.awaitFirstFetch()
        if (disposed.get()) return@LaunchedEffect
        AdManager.loadNativeAd(
            context = context,
            placementKey = placement,
            onAdLoaded = { ad ->
                if (disposed.get()) {
                    ad.destroy()
                } else {
                    nativeAdState.value = ad
                    latestOnLoaded()
                }
            },
            onAdFailed = {
                if (!disposed.get()) latestOnUnavailable()
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            disposed.set(true)
            nativeAdState.value?.destroy()
            nativeAdState.value = null
        }
    }

    return nativeAdState
}

// ---------------------------------------------------------------------------
// 1. Splash Screen - Bottom Native Ad (Placement 1)
// ---------------------------------------------------------------------------

@Composable
fun SplashBottomNativeAd(
    modifier: Modifier = Modifier,
    placement: String = AdManager.NATIVE_SPLASH,
    onAdLoaded: () -> Unit = {},
    onAdUnavailable: () -> Unit = {}
) {
    val nativeAd by rememberNativeAdState(
        placement = placement,
        onAdLoaded = onAdLoaded,
        onAdUnavailable = onAdUnavailable
    )

    if (nativeAd != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color(0xFFFFFFFF))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    createSplashNativeAdView(ctx)
                },
                update = { view ->
                    populateSplashNativeAd(view, nativeAd!!)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun createSplashNativeAdView(context: Context): NativeAdView {
    val nativeAdView = NativeAdView(context)
    nativeAdView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Row: Icon + (Ad Badge & Headline & Advertiser) + CTA Button
    val topRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // App Icon (48dp x 48dp)
    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 48), dpToPx(context, 48))
        scaleType = ImageView.ScaleType.FIT_CENTER
        val bg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 10).toFloat()
            setColor(android.graphics.Color.parseColor("#F1F5F9"))
        }
        background = bg
        clipToOutline = true
    }
    topRow.addView(iconView)
    nativeAdView.iconView = iconView

    // Text Column
    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dpToPx(context, 10), 0, dpToPx(context, 10), 0)
        }
    }

    val badgeAndTitleRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    // Ad Badge (AdMob Standard)
    val badge = TextView(context).apply {
        text = "Ad"
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.parseColor("#1E40AF"))
        val badgeBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 4).toFloat()
            setColor(android.graphics.Color.parseColor("#DBEAFE"))
            setStroke(dpToPx(context, 1), android.graphics.Color.parseColor("#93C5FD"))
        }
        background = badgeBg
        setPadding(dpToPx(context, 5), dpToPx(context, 1), dpToPx(context, 5), dpToPx(context, 1))
    }
    badgeAndTitleRow.addView(badge)

    // Headline
    val headlineView = TextView(context).apply {
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.parseColor("#0F172A"))
        maxLines = 1
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(context, 6)
        }
    }
    badgeAndTitleRow.addView(headlineView)
    nativeAdView.headlineView = headlineView
    textCol.addView(badgeAndTitleRow)

    // Advertiser / Secondary
    val advertiserView = TextView(context).apply {
        textSize = 11.5f
        setTextColor(android.graphics.Color.parseColor("#64748B"))
        maxLines = 1
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 2)
        }
    }
    textCol.addView(advertiserView)
    nativeAdView.advertiserView = advertiserView
    topRow.addView(textCol)

    // CTA Button
    val ctaButton = Button(context).apply {
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.WHITE)
        val ctaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 10).toFloat()
            setColor(android.graphics.Color.parseColor("#1A6BFF"))
        }
        background = ctaBg
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(context, 38)
        )
        setPadding(dpToPx(context, 16), 0, dpToPx(context, 16), 0)
    }
    topRow.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton
    rootLayout.addView(topRow)

    // Body Text Row
    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(android.graphics.Color.parseColor("#475569"))
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 6)
        }
    }
    rootLayout.addView(bodyView)
    nativeAdView.bodyView = bodyView

    nativeAdView.addView(rootLayout)
    return nativeAdView
}

private fun populateSplashNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
    (nativeAdView.advertiserView as? TextView)?.text = nativeAd.advertiser ?: (nativeAd.store ?: "")
    (nativeAdView.bodyView as? TextView)?.text = nativeAd.body ?: ""
    (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction ?: "Install"

    if (nativeAd.icon != null) {
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        nativeAdView.iconView?.visibility = View.VISIBLE
    } else {
        nativeAdView.iconView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}

// ---------------------------------------------------------------------------
// 2. Onboarding Full-Screen Native Ad (Placement 3)
// ---------------------------------------------------------------------------

@Composable
fun FullScreenNativeAdCard(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    placement: String = AdManager.NATIVE_ONBOARDING,
    onAdLoaded: () -> Unit = {}
) {
    // A failed / disabled load also calls onAdLoaded so the onboarding countdown never blocks.
    val nativeAd by rememberNativeAdState(
        placement = placement,
        onAdLoaded = onAdLoaded,
        onAdUnavailable = onAdLoaded
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x22FFFFFF))
            .border(1.5.dp, Color(0x44FFFFFF), RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (nativeAd != null) {
            AndroidView(
                factory = { ctx -> createFullScreenNativeAdView(ctx) },
                update = { view -> populateFullScreenNativeAd(view, nativeAd!!) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("Sponsored Story", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

private fun createFullScreenNativeAdView(context: Context): NativeAdView {
    val nativeAdView = NativeAdView(context)
    nativeAdView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    // Top Header: Badge & Advertiser
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 10)
        }
    }

    val badge = TextView(context).apply {
        text = "Sponsored Ad"
        textSize = 11f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.WHITE)
        val badgeBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 6).toFloat()
            setColor(android.graphics.Color.parseColor("#F59E0B"))
        }
        background = badgeBg
        setPadding(dpToPx(context, 8), dpToPx(context, 2), dpToPx(context, 8), dpToPx(context, 2))
    }
    headerRow.addView(badge)

    val advertiserView = TextView(context).apply {
        textSize = 12f
        setTextColor(android.graphics.Color.argb(200, 255, 255, 255))
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(context, 8)
        }
    }
    headerRow.addView(advertiserView)
    nativeAdView.advertiserView = advertiserView
    rootLayout.addView(headerRow)

    // Media View
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply {
            bottomMargin = dpToPx(context, 12)
        }
        val mediaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 16).toFloat()
            setColor(android.graphics.Color.argb(30, 255, 255, 255))
        }
        background = mediaBg
        clipToOutline = true
    }
    rootLayout.addView(mediaView)
    nativeAdView.mediaView = mediaView

    // Info Row: App Icon + Headline + Rating
    val infoRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 10)
        }
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 48), dpToPx(context, 48))
        scaleType = ImageView.ScaleType.FIT_CENTER
        val bg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 12).toFloat()
            setColor(android.graphics.Color.argb(40, 255, 255, 255))
        }
        background = bg
        clipToOutline = true
    }
    infoRow.addView(iconView)
    nativeAdView.iconView = iconView

    val titleCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(context, 12)
        }
    }

    val headlineView = TextView(context).apply {
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.WHITE)
        maxLines = 1
    }
    titleCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(android.graphics.Color.argb(220, 255, 255, 255))
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 2)
        }
    }
    titleCol.addView(bodyView)
    nativeAdView.bodyView = bodyView
    infoRow.addView(titleCol)
    rootLayout.addView(infoRow)

    // Big Call to Action Button
    val ctaButton = Button(context).apply {
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.parseColor("#1D4ED8"))
        val ctaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 14).toFloat()
            setColor(android.graphics.Color.WHITE)
        }
        background = ctaBg
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 50)
        )
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    return nativeAdView
}

private fun populateFullScreenNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
    (nativeAdView.bodyView as? TextView)?.text = nativeAd.body ?: ""
    (nativeAdView.advertiserView as? TextView)?.text = nativeAd.advertiser ?: ""
    (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction ?: "Install Now"

    if (nativeAd.icon != null) {
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        nativeAdView.iconView?.visibility = View.VISIBLE
    } else {
        nativeAdView.iconView?.visibility = View.GONE
    }

    if (nativeAd.mediaContent != null) {
        nativeAdView.mediaView?.setMediaContent(nativeAd.mediaContent!!)
        nativeAdView.mediaView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}

// ---------------------------------------------------------------------------
// 3. Large Native Ad (Placements 5 & 6: Language & Home Screens)
// ---------------------------------------------------------------------------

@Composable
fun LargeNativeAdCard(
    modifier: Modifier = Modifier,
    placement: String = AdManager.NATIVE_HOME
) {
    val nativeAd by rememberNativeAdState(placement)
    val isDark = AppColors.isDark

    if (nativeAd != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(AppColors.surface)
                .border(1.dp, AppColors.cardBorder, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            AndroidView(
                factory = { ctx -> createLargeNativeAdView(ctx, isDark) },
                update = { view -> populateLargeNativeAd(view, nativeAd!!) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun createLargeNativeAdView(context: Context, isDark: Boolean): NativeAdView {
    val nativeAdView = NativeAdView(context)
    nativeAdView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Header: App Icon + Title / Rating + Ad Badge
    val topHeader = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 10)
        }
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 48), dpToPx(context, 48))
        scaleType = ImageView.ScaleType.FIT_CENTER
        val bg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 10).toFloat()
            setColor(if (isDark) android.graphics.Color.parseColor("#1E293B") else android.graphics.Color.parseColor("#F1F5F9"))
        }
        background = bg
        clipToOutline = true
    }
    topHeader.addView(iconView)
    nativeAdView.iconView = iconView

    val titleCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(context, 10)
            marginEnd = dpToPx(context, 8)
        }
    }

    val headlineView = TextView(context).apply {
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (isDark) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#0F172A"))
        maxLines = 1
    }
    titleCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val subRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 2)
        }
    }

    val advertiserView = TextView(context).apply {
        textSize = 11f
        setTextColor(if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B"))
        maxLines = 1
    }
    subRow.addView(advertiserView)
    nativeAdView.advertiserView = advertiserView
    titleCol.addView(subRow)
    topHeader.addView(titleCol)

    // Ad Badge
    val badge = TextView(context).apply {
        text = "Ad"
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.parseColor("#1E40AF"))
        val badgeBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 4).toFloat()
            setColor(android.graphics.Color.parseColor("#DBEAFE"))
            setStroke(dpToPx(context, 1), android.graphics.Color.parseColor("#93C5FD"))
        }
        background = badgeBg
        setPadding(dpToPx(context, 5), dpToPx(context, 1), dpToPx(context, 5), dpToPx(context, 1))
    }
    topHeader.addView(badge)
    rootLayout.addView(topHeader)

    // Media View
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 160)
        ).apply {
            bottomMargin = dpToPx(context, 8)
        }
        val mediaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 12).toFloat()
            setColor(if (isDark) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.parseColor("#E2E8F0"))
        }
        background = mediaBg
        clipToOutline = true
    }
    rootLayout.addView(mediaView)
    nativeAdView.mediaView = mediaView

    // Body Text
    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(if (isDark) android.graphics.Color.parseColor("#CBD5E1") else android.graphics.Color.parseColor("#475569"))
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 10)
        }
    }
    rootLayout.addView(bodyView)
    nativeAdView.bodyView = bodyView

    // Call to Action Button
    val ctaButton = Button(context).apply {
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.WHITE)
        val ctaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 12).toFloat()
            setColor(android.graphics.Color.parseColor("#1A6BFF"))
        }
        background = ctaBg
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 44)
        )
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    return nativeAdView
}

private fun populateLargeNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
    (nativeAdView.bodyView as? TextView)?.text = nativeAd.body ?: ""
    (nativeAdView.advertiserView as? TextView)?.text = nativeAd.advertiser ?: (nativeAd.store ?: "")
    (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction ?: "Learn More"

    if (nativeAd.icon != null) {
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        nativeAdView.iconView?.visibility = View.VISIBLE
    } else {
        nativeAdView.iconView?.visibility = View.GONE
    }

    if (nativeAd.mediaContent != null) {
        nativeAdView.mediaView?.setMediaContent(nativeAd.mediaContent!!)
        nativeAdView.mediaView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}

// ---------------------------------------------------------------------------
// 4. Medium Native Ad (Placement 7: Download Screen)
// ---------------------------------------------------------------------------

@Composable
fun MediumNativeAdCard(
    modifier: Modifier = Modifier,
    placement: String = AdManager.NATIVE_DOWNLOADS
) {
    val nativeAd by rememberNativeAdState(placement)
    val isDark = AppColors.isDark

    if (nativeAd != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.surface)
                .border(1.dp, AppColors.cardBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            AndroidView(
                factory = { ctx -> createMediumNativeAdView(ctx, isDark) },
                update = { view -> populateMediumNativeAd(view, nativeAd!!) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun createMediumNativeAdView(context: Context, isDark: Boolean): NativeAdView {
    val nativeAdView = NativeAdView(context)
    nativeAdView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Top Header: App Icon + Title / Rating + Ad Badge
    val topHeader = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 8)
        }
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(context, 42), dpToPx(context, 42))
        scaleType = ImageView.ScaleType.FIT_CENTER
        val bg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 10).toFloat()
            setColor(if (isDark) android.graphics.Color.parseColor("#1E293B") else android.graphics.Color.parseColor("#F1F5F9"))
        }
        background = bg
        clipToOutline = true
    }
    topHeader.addView(iconView)
    nativeAdView.iconView = iconView

    val titleCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dpToPx(context, 10)
            marginEnd = dpToPx(context, 8)
        }
    }

    val headlineView = TextView(context).apply {
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (isDark) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#0F172A"))
        maxLines = 1
    }
    titleCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val subRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(context, 2)
        }
    }

    val advertiserView = TextView(context).apply {
        textSize = 11f
        setTextColor(if (isDark) android.graphics.Color.parseColor("#94A3B8") else android.graphics.Color.parseColor("#64748B"))
        maxLines = 1
    }
    subRow.addView(advertiserView)
    nativeAdView.advertiserView = advertiserView
    titleCol.addView(subRow)
    topHeader.addView(titleCol)

    // Ad Badge
    val badge = TextView(context).apply {
        text = "Ad"
        textSize = 10f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.parseColor("#1E40AF"))
        val badgeBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 4).toFloat()
            setColor(android.graphics.Color.parseColor("#DBEAFE"))
            setStroke(dpToPx(context, 1), android.graphics.Color.parseColor("#93C5FD"))
        }
        background = badgeBg
        setPadding(dpToPx(context, 5), dpToPx(context, 1), dpToPx(context, 5), dpToPx(context, 1))
    }
    topHeader.addView(badge)
    rootLayout.addView(topHeader)

    // Media View
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 130)
        ).apply {
            bottomMargin = dpToPx(context, 8)
        }
        val mediaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 12).toFloat()
            setColor(if (isDark) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.parseColor("#E2E8F0"))
        }
        background = mediaBg
        clipToOutline = true
    }
    rootLayout.addView(mediaView)
    nativeAdView.mediaView = mediaView

    // Body Text
    val bodyView = TextView(context).apply {
        textSize = 12f
        setTextColor(if (isDark) android.graphics.Color.parseColor("#CBD5E1") else android.graphics.Color.parseColor("#475569"))
        maxLines = 2
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(context, 8)
        }
    }
    rootLayout.addView(bodyView)
    nativeAdView.bodyView = bodyView

    // Full-Width CTA Button
    val ctaButton = Button(context).apply {
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(android.graphics.Color.WHITE)
        val ctaBg = GradientDrawable().apply {
            cornerRadius = dpToPx(context, 12).toFloat()
            setColor(android.graphics.Color.parseColor("#1A6BFF"))
        }
        background = ctaBg
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dpToPx(context, 42)
        )
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    return nativeAdView
}

private fun populateMediumNativeAd(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline
    (nativeAdView.bodyView as? TextView)?.text = nativeAd.body ?: ""
    (nativeAdView.advertiserView as? TextView)?.text = nativeAd.advertiser ?: (nativeAd.store ?: "")
    (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction ?: "Install Now"

    if (nativeAd.icon != null) {
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        nativeAdView.iconView?.visibility = View.VISIBLE
    } else {
        nativeAdView.iconView?.visibility = View.GONE
    }

    if (nativeAd.mediaContent != null) {
        nativeAdView.mediaView?.setMediaContent(nativeAd.mediaContent!!)
        nativeAdView.mediaView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}

private fun dpToPx(context: Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}
