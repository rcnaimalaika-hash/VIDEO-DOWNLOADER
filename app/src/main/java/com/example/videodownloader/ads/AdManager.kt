package com.example.videodownloader.ads

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.videodownloader.BuildConfig
import com.example.videodownloader.firebase.AnalyticsManager
import com.example.videodownloader.firebase.AppConfig
import com.example.videodownloader.firebase.RemoteConfigManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager handles AdMob initialization, preloading, and presenting
 * Interstitial & Native Ads across all app placements.
 *
 * Everything that marketing may want to tune lives in Firebase Remote Config
 * (see [com.example.videodownloader.firebase.RemoteConfigKeys]):
 *   • master / per-placement on-off switches
 *   • the AdMob ad-unit id used by each placement
 *   • frequency cap + load timeouts
 *
 * Debug builds ALWAYS use Google's sample ad units (AdMob policy), and
 * `use_test_ads` can force them in release for QA / store review.
 *
 * Ensures screens do not advance until the interstitial ad is shown & dismissed,
 * with safety timeouts to prevent getting stuck under any condition.
 */
object AdManager {

    private const val TAG = "AdManager"

    // Official Google AdMob Test Ad Unit IDs
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_AD_UNIT_ID       = "ca-app-pub-3940256099942544/2247696110"

    // Interstitial placement identifiers
    const val PLACEMENT_APP_LAUNCH  = "app_launch"
    const val PLACEMENT_GET_STARTED = "get_started"
    const val PLACEMENT_ON_DOWNLOAD = "on_download"

    // Native placement identifiers
    const val NATIVE_SPLASH     = "native_splash"
    const val NATIVE_ONBOARDING = "native_onboarding"
    const val NATIVE_LANGUAGE   = "native_language"
    const val NATIVE_HOME       = "native_home"
    const val NATIVE_DOWNLOADS  = "native_downloads"

    private const val FORMAT_INTERSTITIAL = "interstitial"
    private const val FORMAT_NATIVE       = "native"

    private val interstitialPlacements = listOf(PLACEMENT_APP_LAUNCH, PLACEMENT_GET_STARTED, PLACEMENT_ON_DOWNLOAD)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isInitialized = AtomicBoolean(false)
    private val lifecycleRegistered = AtomicBoolean(false)
    private val interstitialMap = ConcurrentHashMap<String, InterstitialAd?>()
    private val isLoadingInterstitialMap = ConcurrentHashMap<String, Boolean>()
    private var currentActivityRef: WeakReference<Activity>? = null
    private var appContext: Context? = null

    /** Last time any interstitial was presented (elapsedRealtime), for the frequency cap. */
    @Volatile private var lastInterstitialShownAt = 0L

    // ── Remote Config accessors ──────────────────────────────────────────

    private val config: AppConfig get() = RemoteConfigManager.current

    private val useTestAds: Boolean get() = BuildConfig.DEBUG || config.useTestAds

    fun isInterstitialEnabled(placementKey: String): Boolean = config.isInterstitialEnabled(placementKey)

    fun isNativeEnabled(placementKey: String): Boolean = config.isNativeEnabled(placementKey)

    fun interstitialAdUnitId(placementKey: String): String =
        if (useTestAds) TEST_INTERSTITIAL_AD_UNIT_ID else config.interstitialAdUnitId(placementKey)

    fun nativeAdUnitId(placementKey: String): String =
        if (useTestAds) TEST_NATIVE_AD_UNIT_ID else config.nativeAdUnitId(placementKey)

    /**
     * Called by RemoteConfigManager whenever a new config is activated.
     * Preloads placements that just became enabled and drops cached ads for
     * placements that were switched off.
     */
    fun onConfigChanged(newConfig: AppConfig) {
        val ctx = appContext ?: return
        interstitialPlacements.forEach { placement ->
            if (newConfig.isInterstitialEnabled(placement)) {
                preloadInterstitial(ctx, placement)
            } else {
                interstitialMap.remove(placement)
            }
        }
    }

    // ── Activity tracking ────────────────────────────────────────────────

    /**
     * Explicitly registers the current active Activity.
     */
    fun registerActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        Log.d(TAG, "📌 [registerActivity] Registered active Activity: ${activity::class.java.simpleName}")
    }

    /**
     * Gets the currently active and non-finishing Activity reference.
     */
    fun getCurrentActivity(): Activity? {
        val act = currentActivityRef?.get()
        return if (act != null && !act.isFinishing && !act.isDestroyed) act else null
    }

    /**
     * Initialize MobileAds SDK once on app launch (called from the Application class).
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (context is Activity) {
            registerActivity(context)
        }
        val app = (context.applicationContext as? Application) ?: (context as? Application)
        if (app != null && lifecycleRegistered.compareAndSet(false, true)) {
            app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    registerActivity(activity)
                }
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    registerActivity(activity)
                }
                override fun onActivityStarted(activity: Activity) {
                    registerActivity(activity)
                }
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (currentActivityRef?.get() == activity) {
                        currentActivityRef = null
                    }
                }
            })
        }

        if (isInitialized.compareAndSet(false, true)) {
            MobileAds.initialize(context.applicationContext) { status ->
                Log.d(TAG, "AdMob SDK Initialized: ${status.adapterStatusMap}")
            }
            // Preload critical interstitial placements (each one checks Remote Config)
            interstitialPlacements.forEach { preloadInterstitial(context.applicationContext, it) }
        }
    }

    // ── Interstitials ────────────────────────────────────────────────────

    /**
     * Preloads an Interstitial Ad for the specified placement key.
     */
    fun preloadInterstitial(context: Context, placementKey: String) {
        if (!config.adsEnabled) return
        if (!isInterstitialEnabled(placementKey)) {
            Log.d(TAG, "⏭️ [preloadInterstitial] '$placementKey' disabled by Remote Config")
            return
        }
        if (interstitialMap[placementKey] != null) return
        if (isLoadingInterstitialMap[placementKey] == true) return

        isLoadingInterstitialMap[placementKey] = true
        val adUnitId = interstitialAdUnitId(placementKey)
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial preloaded for placement: $placementKey")
                    attachPaidListener(interstitialAd, placementKey)
                    interstitialMap[placementKey] = interstitialAd
                    isLoadingInterstitialMap[placementKey] = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to preload for $placementKey: ${loadAdError.message}")
                    AnalyticsManager.logAdLoadFailed(FORMAT_INTERSTITIAL, placementKey, loadAdError.code)
                    interstitialMap[placementKey] = null
                    isLoadingInterstitialMap[placementKey] = false
                }
            }
        )
    }

    /**
     * Finds the underlying Activity from any Context, unwrapping ContextWrapper if needed,
     * or falling back to the globally registered active Activity.
     */
    fun Context.findActivity(): Activity? {
        var currentContext: Context? = this
        var depth = 0
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) {
                Log.d(TAG, "🔍 [findActivity] Found Activity: ${currentContext::class.java.simpleName} at depth $depth")
                return currentContext
            }
            currentContext = currentContext.baseContext
            depth++
        }
        if (currentContext is Activity) {
            return currentContext
        }
        val fallback = getCurrentActivity()
        Log.d(TAG, "🔍 [findActivity] Using fallback registered activity: ${fallback?.let { it::class.java.simpleName } ?: "NULL"}")
        return fallback
    }

    /**
     * Overload of showInterstitial taking any Context.
     */
    fun showInterstitial(
        context: Context,
        placementKey: String,
        onDismissed: () -> Unit
    ) {
        Log.d(TAG, "🟢 [showInterstitial] Invoked with Context: $context for placement: '$placementKey'")
        val activity = context.findActivity()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.e(TAG, "❌ [showInterstitial] Cannot find valid non-finishing Activity from context! (activity=$activity)")
            AnalyticsManager.logAdSkipped(FORMAT_INTERSTITIAL, placementKey, "no_activity")
            onDismissed()
            return
        }
        showInterstitial(activity, placementKey, onDismissed)
    }

    /**
     * Shows the Interstitial Ad for the specified placement key.
     * - Skips immediately (calling onDismissed) when the placement is disabled in
     *   Remote Config or the frequency cap has not elapsed.
     * - If the ad is already cached, shows it immediately.
     * - If not cached, shows a brief loading dialog and fetches the ad.
     * Guaranteed to invoke onDismissed() on the main looper once the ad is closed or on error.
     */
    fun showInterstitial(
        activity: Activity,
        placementKey: String,
        onDismissed: () -> Unit
    ) {
        Log.d(TAG, "🎬 [showInterstitial] Showing for placement: '$placementKey' on Activity: ${activity::class.java.simpleName}")

        if (!isInterstitialEnabled(placementKey)) {
            Log.d(TAG, "⏭️ [showInterstitial] '$placementKey' disabled by Remote Config → skipping")
            AnalyticsManager.logAdSkipped(FORMAT_INTERSTITIAL, placementKey, "remote_config_disabled")
            mainHandler.post { onDismissed() }
            return
        }

        val minIntervalMs = config.interstitialMinIntervalSec * 1000
        val sinceLast = SystemClock.elapsedRealtime() - lastInterstitialShownAt
        if (minIntervalMs > 0 && lastInterstitialShownAt > 0 && sinceLast < minIntervalMs) {
            Log.d(TAG, "⏭️ [showInterstitial] Frequency cap: ${sinceLast}ms < ${minIntervalMs}ms → skipping '$placementKey'")
            AnalyticsManager.logAdSkipped(FORMAT_INTERSTITIAL, placementKey, "frequency_cap")
            mainHandler.post { onDismissed() }
            return
        }

        val existingAd = interstitialMap.remove(placementKey)

        if (existingAd != null) {
            Log.d(TAG, "⚡ [showInterstitial] Found preloaded ad in cache for '$placementKey'! Presenting now...")
            presentLoadedInterstitial(activity, existingAd, placementKey, onDismissed)
            return
        }

        Log.d(TAG, "⏳ [showInterstitial] No cached ad for '$placementKey'. Loading on-demand with progress dialog...")

        // Ad is not ready yet: Show a loading progress dialog and load it now
        var loadingDialog: Dialog? = null
        val isCompleted = AtomicBoolean(false)

        fun dismissLoadingDialog() {
            mainHandler.post {
                try {
                    if (loadingDialog?.isShowing == true) {
                        Log.d(TAG, "🚪 [LoadingDialog] Dismissing loading dialog")
                        loadingDialog?.dismiss()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error dismissing loading dialog: ${e.message}")
                }
            }
        }

        fun safeComplete() {
            if (isCompleted.compareAndSet(false, true)) {
                Log.d(TAG, "🛡️ [safeComplete] Completing showInterstitial for '$placementKey'")
                dismissLoadingDialog()
                preloadInterstitial(activity.applicationContext, placementKey)
                mainHandler.post { onDismissed() }
            }
        }

        try {
            if (!activity.isFinishing && !activity.isDestroyed) {
                loadingDialog = createLoadingDialog(activity)
                loadingDialog.show()
                Log.d(TAG, "📋 [LoadingDialog] Showing loading dialog for on-demand ad")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading dialog: ${e.message}")
        }

        // Safety timeout (Remote Config `interstitial_load_timeout_ms`) so the app never hangs
        val timeoutMs = config.interstitialLoadTimeoutMs.coerceIn(1000L, 15000L)
        val timeoutRunnable = Runnable {
            if (!isCompleted.get()) {
                Log.w(TAG, "⏰ [Timeout] Interstitial load timeout (${timeoutMs}ms) reached for placement: '$placementKey'")
                AnalyticsManager.logAdSkipped(FORMAT_INTERSTITIAL, placementKey, "load_timeout")
                safeComplete()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, timeoutMs)

        val adUnitId = interstitialAdUnitId(placementKey)
        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "📡 [showInterstitial] Calling InterstitialAd.load with AdUnit: $adUnitId")
        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "✅ [onAdLoaded] On-demand ad loaded successfully for '$placementKey'!")
                    mainHandler.removeCallbacks(timeoutRunnable)
                    dismissLoadingDialog()
                    attachPaidListener(interstitialAd, placementKey)

                    if (isCompleted.compareAndSet(false, true)) {
                        presentLoadedInterstitial(activity, interstitialAd, placementKey, onDismissed)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ [onAdFailedToLoad] On-demand ad failed for '$placementKey': code=${loadAdError.code}, message=${loadAdError.message}, domain=${loadAdError.domain}")
                    AnalyticsManager.logAdLoadFailed(FORMAT_INTERSTITIAL, placementKey, loadAdError.code)
                    mainHandler.removeCallbacks(timeoutRunnable)
                    safeComplete()
                }
            }
        )
    }

    private fun presentLoadedInterstitial(
        activity: Activity,
        interstitialAd: InterstitialAd,
        placementKey: String,
        onDismissed: () -> Unit
    ) {
        val hasDismissed = AtomicBoolean(false)

        fun triggerDismiss() {
            if (hasDismissed.compareAndSet(false, true)) {
                Log.d(TAG, "🚀 [triggerDismiss] Triggering onDismissed for placement: '$placementKey'")
                preloadInterstitial(activity.applicationContext, placementKey)
                mainHandler.post {
                    onDismissed()
                }
            }
        }

        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "👋 [onAdDismissedFullScreenContent] User dismissed Interstitial for '$placementKey'")
                triggerDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "❌ [onAdFailedToShowFullScreenContent] Failed to show for '$placementKey': code=${adError.code}, message=${adError.message}")
                triggerDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "📺 [onAdShowedFullScreenContent] Interstitial is now visible on screen for '$placementKey'")
                lastInterstitialShownAt = SystemClock.elapsedRealtime()
                AnalyticsManager.logAdShown(FORMAT_INTERSTITIAL, placementKey)
            }

            override fun onAdClicked() {
                AnalyticsManager.logAdClicked(FORMAT_INTERSTITIAL, placementKey)
            }
        }

        if (!activity.isFinishing && !activity.isDestroyed) {
            Log.d(TAG, "▶️ [presentLoadedInterstitial] Calling interstitialAd.show(activity)")
            interstitialAd.show(activity)
        } else {
            Log.w(TAG, "⚠️ [presentLoadedInterstitial] Activity is finishing or destroyed, skipping show")
            triggerDismiss()
        }
    }

    /** Forwards AdMob paid events to GA4 `ad_impression` (Google Ads ROAS signal). */
    private fun attachPaidListener(interstitialAd: InterstitialAd, placementKey: String) {
        interstitialAd.setOnPaidEventListener { adValue ->
            AnalyticsManager.logAdImpression(
                adValue      = adValue,
                adFormat     = FORMAT_INTERSTITIAL,
                adUnitId     = interstitialAd.adUnitId,
                placement    = placementKey,
                responseInfo = interstitialAd.responseInfo
            )
        }
    }

    private fun createLoadingDialog(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(48, 36, 48, 36)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.parseColor("#1E293B"))
            }
            background = bg
        }

        val progressBar = ProgressBar(context).apply {
            val params = LinearLayout.LayoutParams(60, 60)
            layoutParams = params
            isIndeterminate = true
        }
        layout.addView(progressBar)

        val textView = TextView(context).apply {
            text = "Loading ad…"
            textSize = 15f
            setTextColor(Color.WHITE)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 28
            }
            layoutParams = params
        }
        layout.addView(textView)

        dialog.setContentView(layout)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    // ── Native ads ───────────────────────────────────────────────────────

    /**
     * Loads a NativeAd for [placementKey]. The ad-unit id comes from Remote
     * Config; when the placement is disabled `onAdFailed` is invoked with
     * `null` right away so callers can collapse the slot.
     */
    fun loadNativeAd(
        context: Context,
        placementKey: String = NATIVE_HOME,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: ((LoadAdError?) -> Unit)? = null
    ) {
        if (!isNativeEnabled(placementKey)) {
            Log.d(TAG, "⏭️ [loadNativeAd] '$placementKey' disabled by Remote Config")
            AnalyticsManager.logAdSkipped(FORMAT_NATIVE, placementKey, "remote_config_disabled")
            onAdFailed?.invoke(null)
            return
        }

        val adUnitId = nativeAdUnitId(placementKey)
        val builder = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad loaded successfully for '$placementKey'.")
                nativeAd.setOnPaidEventListener { adValue ->
                    AnalyticsManager.logAdImpression(
                        adValue      = adValue,
                        adFormat     = FORMAT_NATIVE,
                        adUnitId     = adUnitId,
                        placement    = placementKey,
                        responseInfo = nativeAd.responseInfo
                    )
                }
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load for '$placementKey': ${error.message}")
                    AnalyticsManager.logAdLoadFailed(FORMAT_NATIVE, placementKey, error.code)
                    onAdFailed?.invoke(error)
                }

                override fun onAdImpression() {
                    AnalyticsManager.logAdShown(FORMAT_NATIVE, placementKey)
                }

                override fun onAdClicked() {
                    AnalyticsManager.logAdClicked(FORMAT_NATIVE, placementKey)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                    .build()
            )

        builder.build().loadAd(AdRequest.Builder().build())
    }
}
