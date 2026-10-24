package com.example.videodownloader.firebase

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.example.videodownloader.BuildConfig
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.ResponseInfo
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Central Firebase Analytics façade. Every marketing / product event the app
 * emits goes through here so event names and parameters stay consistent —
 * these names are what you will see in GA4 and import into Google Ads as
 * conversions ("key events").
 *
 * Uses GA4 *recommended* event names wherever one exists (tutorial_begin,
 * tutorial_complete, share, view_item, select_item, begin_checkout, purchase,
 * ad_impression, screen_view) because Google Ads / GA4 give those built-in
 * reporting and conversion modelling. Everything else is a custom event.
 */
object AnalyticsManager {

    private const val TAG = "Analytics"

    private var analytics: FirebaseAnalytics? = null

    // ── Event names ──────────────────────────────────────────────────────
    object Event {
        // GA4 recommended
        const val SCREEN_VIEW        = FirebaseAnalytics.Event.SCREEN_VIEW
        const val TUTORIAL_BEGIN     = FirebaseAnalytics.Event.TUTORIAL_BEGIN
        const val TUTORIAL_COMPLETE  = FirebaseAnalytics.Event.TUTORIAL_COMPLETE
        const val SHARE              = FirebaseAnalytics.Event.SHARE
        const val VIEW_ITEM          = FirebaseAnalytics.Event.VIEW_ITEM
        const val SELECT_ITEM        = FirebaseAnalytics.Event.SELECT_ITEM
        const val BEGIN_CHECKOUT     = FirebaseAnalytics.Event.BEGIN_CHECKOUT
        const val PURCHASE           = FirebaseAnalytics.Event.PURCHASE
        const val AD_IMPRESSION      = FirebaseAnalytics.Event.AD_IMPRESSION

        // Custom — core product funnel
        const val DOWNLOAD_START     = "download_start"
        const val DOWNLOAD_COMPLETE  = "download_complete"
        const val DOWNLOAD_FAILED    = "download_failed"
        const val DOWNLOAD_RETRY     = "download_retry"
        const val DOWNLOAD_CANCEL    = "download_cancel"
        const val MEDIA_OPENED       = "media_opened"
        const val LANGUAGE_SELECTED  = "language_selected"

        // Custom — growth / retention
        const val RATE_PROMPT_SHOWN  = "rate_prompt_shown"
        const val RATE_SUBMITTED     = "rate_submitted"
        const val RATE_LATER         = "rate_later"
        const val PRO_SCREEN_VIEW    = "pro_screen_view"
        const val PRO_PLAN_SELECTED  = "pro_plan_selected"
        const val THEME_CHANGED      = "theme_changed"

        // Custom — ads (monetisation diagnostics)
        const val AD_SHOWN           = "ad_shown"
        const val AD_CLICKED         = "ad_clicked"
        const val AD_LOAD_FAILED     = "ad_load_failed"
        const val AD_SKIPPED         = "ad_skipped"

        // Custom — push
        const val PUSH_RECEIVED      = "push_received"
        const val PUSH_OPENED        = "push_opened"
        const val NOTIFICATION_PERMISSION = "notification_permission"
    }

    // ── Parameter names ──────────────────────────────────────────────────
    object Param {
        const val PLATFORM        = "platform"        // youtube / instagram / tiktok … (host only, never the URL)
        const val FILE_TYPE       = "file_type"       // video | audio
        const val FORMAT          = "format"          // best | worst
        const val DURATION_MS     = "duration_ms"
        const val FILE_SIZE_BYTES = "file_size_bytes"
        const val REASON          = "reason"
        const val LANGUAGE        = "language"
        const val RATING          = "rating"
        const val PLAN            = "plan"
        const val PLACEMENT       = "placement"
        const val AD_FORMAT       = FirebaseAnalytics.Param.AD_FORMAT
        const val ERROR_CODE      = "error_code"
        const val CAMPAIGN        = "campaign"
        const val MESSAGE_ID      = "message_id"
        const val SCREEN          = "screen"
        const val GRANTED         = "granted"
        const val THEME           = "theme"
        const val SOURCE          = "source"
        const val METHOD          = FirebaseAnalytics.Param.METHOD
        const val CONTENT_TYPE    = FirebaseAnalytics.Param.CONTENT_TYPE
    }

    // ── User properties (usable as GA4 audiences + Remote Config conditions) ──
    object UserProperty {
        const val APP_LANGUAGE = "app_language"
        const val THEME        = "theme"
        const val IS_PRO       = "is_pro"
        const val BUILD_TYPE   = "build_type"
        const val ADS_ENABLED  = "ads_enabled"
    }

    private const val CURRENCY_USD = "USD"

    // ── Lifecycle ────────────────────────────────────────────────────────

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
        applyCollection(RemoteConfigManager.current)
        setUserProperty(UserProperty.BUILD_TYPE, if (BuildConfig.DEBUG) "debug" else "release")
        setUserProperty(UserProperty.IS_PRO, "false")
    }

    internal fun onConfigChanged(config: AppConfig) {
        applyCollection(config)
        setUserProperty(UserProperty.ADS_ENABLED, config.adsEnabled.toString())
    }

    private fun applyCollection(config: AppConfig) {
        analytics?.setAnalyticsCollectionEnabled(config.analyticsEnabled)
    }

    // ── Low-level ────────────────────────────────────────────────────────

    fun logEvent(name: String, params: Bundle? = null) {
        val fa = analytics ?: run { Log.w(TAG, "logEvent($name) before init — dropped"); return }
        fa.logEvent(name, params)
        if (BuildConfig.DEBUG) Log.d(TAG, "event=$name ${params?.let { bundleToString(it) } ?: ""}")
    }

    fun setUserProperty(name: String, value: String?) {
        analytics?.setUserProperty(name, value)
    }

    fun setUserId(userId: String?) {
        analytics?.setUserId(userId)
    }

    /**
     * Hook for a future UMP / GDPR consent flow. Call once the user has
     * answered the consent form; Firebase then respects the choice for
     * Google Ads personalisation & measurement.
     */
    fun setAdConsent(granted: Boolean) {
        val status = if (granted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
        analytics?.setConsent(
            mapOf(
                FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to status,
                FirebaseAnalytics.ConsentType.AD_STORAGE to status,
                FirebaseAnalytics.ConsentType.AD_USER_DATA to status,
                FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to status
            )
        )
    }

    // ── Screens ──────────────────────────────────────────────────────────

    fun logScreenView(screenName: String, screenClass: String = "MainActivity") {
        logEvent(Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        })
        CrashReporter.setScreen(screenName)
    }

    // ── Onboarding ───────────────────────────────────────────────────────

    fun logTutorialBegin() = logEvent(Event.TUTORIAL_BEGIN)

    fun logTutorialComplete() = logEvent(Event.TUTORIAL_COMPLETE)

    fun logLanguageSelected(language: String) {
        logEvent(Event.LANGUAGE_SELECTED, Bundle().apply { putString(Param.LANGUAGE, language) })
        setUserProperty(UserProperty.APP_LANGUAGE, language)
    }

    // ── Download funnel ──────────────────────────────────────────────────

    fun logDownloadStart(url: String, fileType: String, format: String) {
        logEvent(Event.DOWNLOAD_START, Bundle().apply {
            putString(Param.PLATFORM, platformOf(url))
            putString(Param.FILE_TYPE, fileType)
            putString(Param.FORMAT, format)
        })
    }

    fun logDownloadComplete(url: String, fileType: String, format: String, durationMs: Long, fileSizeBytes: Long) {
        logEvent(Event.DOWNLOAD_COMPLETE, Bundle().apply {
            putString(Param.PLATFORM, platformOf(url))
            putString(Param.FILE_TYPE, fileType)
            putString(Param.FORMAT, format)
            putLong(Param.DURATION_MS, durationMs)
            putLong(Param.FILE_SIZE_BYTES, fileSizeBytes)
        })
    }

    fun logDownloadFailed(url: String, fileType: String, reason: String?) {
        logEvent(Event.DOWNLOAD_FAILED, Bundle().apply {
            putString(Param.PLATFORM, platformOf(url))
            putString(Param.FILE_TYPE, fileType)
            putString(Param.REASON, (reason ?: "unknown").take(100))
        })
    }

    fun logDownloadRetry(url: String, fileType: String) {
        logEvent(Event.DOWNLOAD_RETRY, Bundle().apply {
            putString(Param.PLATFORM, platformOf(url))
            putString(Param.FILE_TYPE, fileType)
        })
    }

    fun logDownloadCancel(url: String, fileType: String) {
        logEvent(Event.DOWNLOAD_CANCEL, Bundle().apply {
            putString(Param.PLATFORM, platformOf(url))
            putString(Param.FILE_TYPE, fileType)
        })
    }

    fun logMediaOpened(fileType: String) {
        logEvent(Event.MEDIA_OPENED, Bundle().apply { putString(Param.FILE_TYPE, fileType) })
    }

    // ── Share ────────────────────────────────────────────────────────────

    /** App invite / referral share. [method] = WhatsApp, Facebook, … or "other". */
    fun logShareApp(method: String) {
        logEvent(Event.SHARE, Bundle().apply {
            putString(Param.CONTENT_TYPE, "app_invite")
            putString(Param.METHOD, method)
            putString(FirebaseAnalytics.Param.ITEM_ID, "app")
        })
    }

    /** Sharing a downloaded file. */
    fun logShareMedia(fileType: String) {
        logEvent(Event.SHARE, Bundle().apply {
            putString(Param.CONTENT_TYPE, "downloaded_media")
            putString(Param.METHOD, "system_share_sheet")
            putString(FirebaseAnalytics.Param.ITEM_ID, fileType)
        })
    }

    // ── Rate app ─────────────────────────────────────────────────────────

    fun logRatePromptShown() = logEvent(Event.RATE_PROMPT_SHOWN)

    fun logRateSubmitted(rating: Int) {
        logEvent(Event.RATE_SUBMITTED, Bundle().apply { putLong(Param.RATING, rating.toLong()) })
    }

    fun logRateLater() = logEvent(Event.RATE_LATER)

    // ── Pro / purchase funnel (GA4 e-commerce shape so Google Ads can value it) ──

    private fun planItem(plan: String, price: Double): Bundle = Bundle().apply {
        putString(FirebaseAnalytics.Param.ITEM_ID, "pro_${plan.lowercase()}")
        putString(FirebaseAnalytics.Param.ITEM_NAME, "Pro $plan")
        putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "subscription")
        putDouble(FirebaseAnalytics.Param.PRICE, price)
        putLong(FirebaseAnalytics.Param.QUANTITY, 1)
    }

    fun logProScreenView(source: String) {
        logEvent(Event.PRO_SCREEN_VIEW, Bundle().apply { putString(Param.SOURCE, source) })
        logEvent(Event.VIEW_ITEM, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, CURRENCY_USD)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(planItem("Yearly", 14.99)))
        })
    }

    fun logProPlanSelected(plan: String, price: Double) {
        logEvent(Event.PRO_PLAN_SELECTED, Bundle().apply { putString(Param.PLAN, plan) })
        logEvent(Event.SELECT_ITEM, Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_LIST_ID, "pro_plans")
            putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, "Pro plans")
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(planItem(plan, price)))
        })
    }

    fun logProCheckoutStarted(plan: String, price: Double) {
        logEvent(Event.BEGIN_CHECKOUT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, CURRENCY_USD)
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            putString(Param.PLAN, plan)
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(planItem(plan, price)))
        })
    }

    /**
     * Call from the Google Play Billing purchase-acknowledged callback once
     * billing is integrated. `purchase` is the canonical Google Ads
     * revenue conversion.
     */
    fun logPurchase(plan: String, price: Double, currency: String = CURRENCY_USD, transactionId: String? = null) {
        logEvent(Event.PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, price)
            transactionId?.let { putString(FirebaseAnalytics.Param.TRANSACTION_ID, it) }
            putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(planItem(plan, price)))
        })
        setUserProperty(UserProperty.IS_PRO, "true")
    }

    // ── Ads ──────────────────────────────────────────────────────────────

    /**
     * Ad-revenue event in the exact shape Google documents for AdMob →
     * GA4 → Google Ads ROAS bidding ("ad_impression" with value/currency).
     */
    fun logAdImpression(
        adValue: AdValue,
        adFormat: String,
        adUnitId: String,
        placement: String,
        responseInfo: ResponseInfo?
    ) {
        logEvent(Event.AD_IMPRESSION, Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob")
            putString(
                FirebaseAnalytics.Param.AD_SOURCE,
                responseInfo?.loadedAdapterResponseInfo?.adSourceName ?: "AdMob"
            )
            putString(FirebaseAnalytics.Param.AD_FORMAT, adFormat)
            putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId)
            putDouble(FirebaseAnalytics.Param.VALUE, adValue.valueMicros / 1_000_000.0)
            putString(FirebaseAnalytics.Param.CURRENCY, adValue.currencyCode)
            putString(Param.PLACEMENT, placement)
        })
    }

    fun logAdShown(adFormat: String, placement: String) {
        logEvent(Event.AD_SHOWN, adBundle(adFormat, placement))
    }

    fun logAdClicked(adFormat: String, placement: String) {
        logEvent(Event.AD_CLICKED, adBundle(adFormat, placement))
    }

    fun logAdLoadFailed(adFormat: String, placement: String, errorCode: Int) {
        logEvent(Event.AD_LOAD_FAILED, adBundle(adFormat, placement).apply {
            putLong(Param.ERROR_CODE, errorCode.toLong())
        })
    }

    /** Ad intentionally not shown: remote-config disabled, frequency cap, no activity … */
    fun logAdSkipped(adFormat: String, placement: String, reason: String) {
        logEvent(Event.AD_SKIPPED, adBundle(adFormat, placement).apply { putString(Param.REASON, reason) })
    }

    private fun adBundle(adFormat: String, placement: String) = Bundle().apply {
        putString(Param.AD_FORMAT, adFormat)
        putString(Param.PLACEMENT, placement)
    }

    // ── Push ─────────────────────────────────────────────────────────────

    fun logPushReceived(campaign: String?, messageId: String?) {
        logEvent(Event.PUSH_RECEIVED, Bundle().apply {
            campaign?.let { putString(Param.CAMPAIGN, it) }
            messageId?.let { putString(Param.MESSAGE_ID, it) }
        })
    }

    fun logPushOpened(campaign: String?, screen: String?) {
        logEvent(Event.PUSH_OPENED, Bundle().apply {
            campaign?.let { putString(Param.CAMPAIGN, it) }
            screen?.let { putString(Param.SCREEN, it) }
        })
    }

    fun logNotificationPermission(granted: Boolean) {
        logEvent(Event.NOTIFICATION_PERMISSION, Bundle().apply { putString(Param.GRANTED, granted.toString()) })
    }

    // ── Misc ─────────────────────────────────────────────────────────────

    fun logThemeChanged(dark: Boolean) {
        val theme = if (dark) "dark" else "light"
        logEvent(Event.THEME_CHANGED, Bundle().apply { putString(Param.THEME, theme) })
        setUserProperty(UserProperty.THEME, theme)
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Reduces a media URL to a short platform label ("youtube", "instagram",
     * "tiktok", …). We never send the full URL to analytics.
     */
    fun platformOf(url: String): String {
        val host = try { Uri.parse(url).host?.lowercase() } catch (_: Exception) { null } ?: return "unknown"
        val stripped = host.removePrefix("www.").removePrefix("m.").removePrefix("mobile.")
        val known = listOf(
            "youtube", "youtu.be", "instagram", "tiktok", "facebook", "fb.watch", "twitter", "x.com",
            "vimeo", "dailymotion", "reddit", "pinterest", "snapchat", "likee", "twitch", "soundcloud"
        )
        val match = known.firstOrNull { stripped.contains(it) }
        return when (match) {
            null       -> stripped.substringBefore('.').take(24).ifBlank { "unknown" }
            "youtu.be" -> "youtube"
            "fb.watch" -> "facebook"
            "x.com"    -> "twitter"
            else       -> match
        }
    }

    @Suppress("DEPRECATION")   // Bundle.get(key) is fine for debug-only logging of mixed types
    private fun bundleToString(b: Bundle): String =
        b.keySet().joinToString(prefix = "{", postfix = "}") { k -> "$k=${b.get(k)}" }
}
