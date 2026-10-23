package com.example.videodownloader.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager handles AdMob initialization, test ad unit IDs, preloading,
 * and presenting Interstitial & Native Ads across all app placements.
 *
 * Ensures screens do not advance until the interstitial ad is shown & dismissed,
 * with safety timeouts to prevent getting stuck under any condition.
 */
object AdManager {

    private const val TAG = "AdManager"

    // Official Google AdMob Test Ad Unit IDs
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_NATIVE_AD_UNIT_ID       = "ca-app-pub-3940256099942544/2247696110"

    // Placement identifiers
    const val PLACEMENT_APP_LAUNCH  = "app_launch"
    const val PLACEMENT_GET_STARTED = "get_started"
    const val PLACEMENT_ON_DOWNLOAD = "on_download"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isInitialized = AtomicBoolean(false)
    private val interstitialMap = ConcurrentHashMap<String, InterstitialAd?>()
    private val isLoadingInterstitialMap = ConcurrentHashMap<String, Boolean>()
    private var currentActivityRef: WeakReference<Activity>? = null

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
     * Initialize MobileAds SDK once on app launch.
     */
    fun initialize(context: Context) {
        if (context is Activity) {
            registerActivity(context)
        }
        val app = (context.applicationContext as? Application) ?: (context as? Application)
        app?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
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

        if (isInitialized.compareAndSet(false, true)) {
            MobileAds.initialize(context.applicationContext) { status ->
                Log.d(TAG, "AdMob SDK Initialized: ${status.adapterStatusMap}")
            }
            // Preload critical interstitial placements
            preloadInterstitial(context.applicationContext, PLACEMENT_APP_LAUNCH)
            preloadInterstitial(context.applicationContext, PLACEMENT_GET_STARTED)
            preloadInterstitial(context.applicationContext, PLACEMENT_ON_DOWNLOAD)
        }
    }

    /**
     * Preloads an Interstitial Ad for the specified placement key.
     */
    fun preloadInterstitial(context: Context, placementKey: String) {
        if (interstitialMap[placementKey] != null) return
        if (isLoadingInterstitialMap[placementKey] == true) return

        isLoadingInterstitialMap[placementKey] = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial preloaded for placement: $placementKey")
                    interstitialMap[placementKey] = interstitialAd
                    isLoadingInterstitialMap[placementKey] = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to preload for $placementKey: ${loadAdError.message}")
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
            onDismissed()
            return
        }
        showInterstitial(activity, placementKey, onDismissed)
    }

    /**
     * Shows the Interstitial Ad for the specified placement key.
     * If the ad is already cached, shows it immediately.
     * If not cached, shows a brief loading dialog and fetches the ad.
     * Guaranteed to invoke onDismissed() on the main looper once the ad is closed or on error.
     */
    fun showInterstitial(
        activity: Activity,
        placementKey: String,
        onDismissed: () -> Unit
    ) {
        Log.d(TAG, "🎬 [showInterstitial] Showing for placement: '$placementKey' on Activity: ${activity::class.java.simpleName}")
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

        // Safety timeout runnable (4.5 seconds max) so app never hangs
        val timeoutRunnable = Runnable {
            if (!isCompleted.get()) {
                Log.w(TAG, "⏰ [Timeout] Interstitial load timeout reached for placement: '$placementKey'")
                safeComplete()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, 4500)

        val adRequest = AdRequest.Builder().build()
        Log.d(TAG, "📡 [showInterstitial] Calling InterstitialAd.load with AdUnit: $TEST_INTERSTITIAL_AD_UNIT_ID")
        InterstitialAd.load(
            activity,
            TEST_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "✅ [onAdLoaded] On-demand ad loaded successfully for '$placementKey'!")
                    mainHandler.removeCallbacks(timeoutRunnable)
                    dismissLoadingDialog()

                    if (isCompleted.compareAndSet(false, true)) {
                        presentLoadedInterstitial(activity, interstitialAd, placementKey, onDismissed)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ [onAdFailedToLoad] On-demand ad failed for '$placementKey': code=${loadAdError.code}, message=${loadAdError.message}, domain=${loadAdError.domain}")
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

    /**
     * Loads a NativeAd instance.
     */
    fun loadNativeAd(
        context: Context,
        adUnitId: String = TEST_NATIVE_AD_UNIT_ID,
        onAdLoaded: (NativeAd) -> Unit,
        onAdFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val builder = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "Native ad loaded successfully.")
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                    onAdFailed?.invoke(error)
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
