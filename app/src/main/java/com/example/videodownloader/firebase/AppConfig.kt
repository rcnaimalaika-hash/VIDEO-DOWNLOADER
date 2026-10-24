package com.example.videodownloader.firebase

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.example.videodownloader.ads.AdManager

/**
 * Immutable, typed snapshot of the Remote Config values the app cares about.
 * Exposed by [RemoteConfigManager.config] as a StateFlow so Compose screens
 * and the ad layer react instantly when a new config is activated.
 */
data class AppConfig(
    // master
    val adsEnabled: Boolean,
    val interstitialAdsEnabled: Boolean,
    val nativeAdsEnabled: Boolean,
    val useTestAds: Boolean,
    // placements
    val splashNativeEnabled: Boolean,
    val appLaunchInterstitialEnabled: Boolean,
    val onboardingNativeEnabled: Boolean,
    val getStartedInterstitialEnabled: Boolean,
    val languageNativeEnabled: Boolean,
    val homeNativeEnabled: Boolean,
    val downloadsNativeEnabled: Boolean,
    val onDownloadInterstitialEnabled: Boolean,
    // ad unit ids
    val interstitialAppLaunchId: String,
    val interstitialGetStartedId: String,
    val interstitialOnDownloadId: String,
    val nativeSplashId: String,
    val nativeOnboardingId: String,
    val nativeLanguageId: String,
    val nativeHomeId: String,
    val nativeDownloadsId: String,
    // behaviour
    val interstitialMinIntervalSec: Long,
    val interstitialLoadTimeoutMs: Long,
    val splashAdDisplayMs: Long,
    val splashTimeoutMs: Long,
    val splashNoAdDelayMs: Long,
    val onboardingAdCountdownSec: Long,
    // features
    val proScreenEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val crashlyticsEnabled: Boolean,
    val pushTopics: List<String>,
    /** true once this snapshot comes from Firebase (fetched or cached), false while on in-app defaults */
    val isFromRemote: Boolean
) {

    // ── Placement helpers (observable from Compose because they live on the snapshot) ──

    fun isInterstitialEnabled(placement: String): Boolean {
        if (!adsEnabled || !interstitialAdsEnabled) return false
        return when (placement) {
            AdManager.PLACEMENT_APP_LAUNCH  -> appLaunchInterstitialEnabled
            AdManager.PLACEMENT_GET_STARTED -> getStartedInterstitialEnabled
            AdManager.PLACEMENT_ON_DOWNLOAD -> onDownloadInterstitialEnabled
            else -> true
        }
    }

    fun isNativeEnabled(placement: String): Boolean {
        if (!adsEnabled || !nativeAdsEnabled) return false
        return when (placement) {
            AdManager.NATIVE_SPLASH     -> splashNativeEnabled
            AdManager.NATIVE_ONBOARDING -> onboardingNativeEnabled
            AdManager.NATIVE_LANGUAGE   -> languageNativeEnabled
            AdManager.NATIVE_HOME       -> homeNativeEnabled
            AdManager.NATIVE_DOWNLOADS  -> downloadsNativeEnabled
            else -> true
        }
    }

    fun interstitialAdUnitId(placement: String): String = when (placement) {
        AdManager.PLACEMENT_APP_LAUNCH  -> interstitialAppLaunchId
        AdManager.PLACEMENT_GET_STARTED -> interstitialGetStartedId
        AdManager.PLACEMENT_ON_DOWNLOAD -> interstitialOnDownloadId
        else -> interstitialAppLaunchId
    }.ifBlank { AdManager.TEST_INTERSTITIAL_AD_UNIT_ID }

    fun nativeAdUnitId(placement: String): String = when (placement) {
        AdManager.NATIVE_SPLASH     -> nativeSplashId
        AdManager.NATIVE_ONBOARDING -> nativeOnboardingId
        AdManager.NATIVE_LANGUAGE   -> nativeLanguageId
        AdManager.NATIVE_HOME       -> nativeHomeId
        AdManager.NATIVE_DOWNLOADS  -> nativeDownloadsId
        else -> nativeHomeId
    }.ifBlank { AdManager.TEST_NATIVE_AD_UNIT_ID }

    companion object {

        /** Minimal reader abstraction so the same mapping works for defaults and Firebase. */
        private interface Reader {
            fun bool(key: String): Boolean
            fun string(key: String): String
            fun long(key: String): Long
        }

        private fun build(r: Reader, fromRemote: Boolean) = AppConfig(
            adsEnabled                     = r.bool(RemoteConfigKeys.ADS_ENABLED),
            interstitialAdsEnabled         = r.bool(RemoteConfigKeys.INTERSTITIAL_ADS_ENABLED),
            nativeAdsEnabled               = r.bool(RemoteConfigKeys.NATIVE_ADS_ENABLED),
            useTestAds                     = r.bool(RemoteConfigKeys.USE_TEST_ADS),

            splashNativeEnabled            = r.bool(RemoteConfigKeys.AD_SPLASH_NATIVE_ENABLED),
            appLaunchInterstitialEnabled   = r.bool(RemoteConfigKeys.AD_APP_LAUNCH_INTERSTITIAL_ENABLED),
            onboardingNativeEnabled        = r.bool(RemoteConfigKeys.AD_ONBOARDING_NATIVE_ENABLED),
            getStartedInterstitialEnabled  = r.bool(RemoteConfigKeys.AD_GET_STARTED_INTERSTITIAL_ENABLED),
            languageNativeEnabled          = r.bool(RemoteConfigKeys.AD_LANGUAGE_NATIVE_ENABLED),
            homeNativeEnabled              = r.bool(RemoteConfigKeys.AD_HOME_NATIVE_ENABLED),
            downloadsNativeEnabled         = r.bool(RemoteConfigKeys.AD_DOWNLOADS_NATIVE_ENABLED),
            onDownloadInterstitialEnabled  = r.bool(RemoteConfigKeys.AD_ON_DOWNLOAD_INTERSTITIAL_ENABLED),

            interstitialAppLaunchId        = r.string(RemoteConfigKeys.ADMOB_INTERSTITIAL_APP_LAUNCH_ID),
            interstitialGetStartedId       = r.string(RemoteConfigKeys.ADMOB_INTERSTITIAL_GET_STARTED_ID),
            interstitialOnDownloadId       = r.string(RemoteConfigKeys.ADMOB_INTERSTITIAL_ON_DOWNLOAD_ID),
            nativeSplashId                 = r.string(RemoteConfigKeys.ADMOB_NATIVE_SPLASH_ID),
            nativeOnboardingId             = r.string(RemoteConfigKeys.ADMOB_NATIVE_ONBOARDING_ID),
            nativeLanguageId               = r.string(RemoteConfigKeys.ADMOB_NATIVE_LANGUAGE_ID),
            nativeHomeId                   = r.string(RemoteConfigKeys.ADMOB_NATIVE_HOME_ID),
            nativeDownloadsId              = r.string(RemoteConfigKeys.ADMOB_NATIVE_DOWNLOADS_ID),

            interstitialMinIntervalSec     = r.long(RemoteConfigKeys.INTERSTITIAL_MIN_INTERVAL_SEC),
            interstitialLoadTimeoutMs      = r.long(RemoteConfigKeys.INTERSTITIAL_LOAD_TIMEOUT_MS),
            splashAdDisplayMs              = r.long(RemoteConfigKeys.SPLASH_AD_DISPLAY_MS),
            splashTimeoutMs                = r.long(RemoteConfigKeys.SPLASH_TIMEOUT_MS),
            splashNoAdDelayMs              = r.long(RemoteConfigKeys.SPLASH_NO_AD_DELAY_MS),
            onboardingAdCountdownSec       = r.long(RemoteConfigKeys.ONBOARDING_AD_COUNTDOWN_SEC),

            proScreenEnabled               = r.bool(RemoteConfigKeys.PRO_SCREEN_ENABLED),
            analyticsEnabled               = r.bool(RemoteConfigKeys.ANALYTICS_ENABLED),
            crashlyticsEnabled             = r.bool(RemoteConfigKeys.CRASHLYTICS_ENABLED),
            pushTopics                     = r.string(RemoteConfigKeys.PUSH_TOPICS)
                                                .split(',').map { it.trim() }.filter { it.isNotEmpty() },
            isFromRemote                   = fromRemote
        )

        /** Snapshot built purely from [RemoteConfigKeys.DEFAULTS]. */
        fun fromDefaults(): AppConfig = build(object : Reader {
            override fun bool(key: String)   = RemoteConfigKeys.DEFAULTS[key] as? Boolean ?: false
            override fun string(key: String) = RemoteConfigKeys.DEFAULTS[key] as? String ?: ""
            override fun long(key: String)   = (RemoteConfigKeys.DEFAULTS[key] as? Number)?.toLong() ?: 0L
        }, fromRemote = false)

        /** Snapshot read from the currently *activated* Firebase values (falls back to defaults per key). */
        fun fromRemote(rc: FirebaseRemoteConfig): AppConfig = build(object : Reader {
            override fun bool(key: String)   = rc.getBoolean(key)
            override fun string(key: String) = rc.getString(key)
            override fun long(key: String)   = rc.getLong(key)
        }, fromRemote = true)
    }
}
