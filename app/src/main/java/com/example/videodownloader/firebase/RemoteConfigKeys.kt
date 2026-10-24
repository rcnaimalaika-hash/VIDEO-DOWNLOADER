package com.example.videodownloader.firebase

import com.example.videodownloader.ads.AdManager

/**
 * Single source of truth for every Firebase Remote Config key the app reads,
 * together with the in-app default that is used until the first fetch succeeds
 * (or forever, if the key is never published in the Firebase console).
 *
 * Keep this file in sync with `firebase/remoteconfig.template.json` — that file
 * is what you publish to the Firebase console (`firebase deploy --only remoteconfig`).
 *
 * Naming convention
 *   ads_*        master switches
 *   ad_<placement>_enabled   per-placement on/off
 *   admob_<placement>_id     AdMob ad UNIT id for that placement
 *   *_ms / *_sec             timing knobs
 */
object RemoteConfigKeys {

    // ── Master switches ──────────────────────────────────────────────────
    const val ADS_ENABLED               = "ads_enabled"
    const val INTERSTITIAL_ADS_ENABLED  = "interstitial_ads_enabled"
    const val NATIVE_ADS_ENABLED        = "native_ads_enabled"
    /** Force Google test ad units even in release (useful for store review / QA). */
    const val USE_TEST_ADS              = "use_test_ads"

    // ── Per-placement on/off ─────────────────────────────────────────────
    const val AD_SPLASH_NATIVE_ENABLED              = "ad_splash_native_enabled"
    const val AD_APP_LAUNCH_INTERSTITIAL_ENABLED    = "ad_app_launch_interstitial_enabled"
    const val AD_ONBOARDING_NATIVE_ENABLED          = "ad_onboarding_native_enabled"
    const val AD_GET_STARTED_INTERSTITIAL_ENABLED   = "ad_get_started_interstitial_enabled"
    const val AD_LANGUAGE_NATIVE_ENABLED            = "ad_language_native_enabled"
    const val AD_HOME_NATIVE_ENABLED                = "ad_home_native_enabled"
    const val AD_DOWNLOADS_NATIVE_ENABLED           = "ad_downloads_native_enabled"
    const val AD_ON_DOWNLOAD_INTERSTITIAL_ENABLED   = "ad_on_download_interstitial_enabled"

    // ── AdMob ad UNIT ids (one per placement, as AdMob recommends) ───────
    const val ADMOB_INTERSTITIAL_APP_LAUNCH_ID  = "admob_interstitial_app_launch_id"
    const val ADMOB_INTERSTITIAL_GET_STARTED_ID = "admob_interstitial_get_started_id"
    const val ADMOB_INTERSTITIAL_ON_DOWNLOAD_ID = "admob_interstitial_on_download_id"
    const val ADMOB_NATIVE_SPLASH_ID            = "admob_native_splash_id"
    const val ADMOB_NATIVE_ONBOARDING_ID        = "admob_native_onboarding_id"
    const val ADMOB_NATIVE_LANGUAGE_ID          = "admob_native_language_id"
    const val ADMOB_NATIVE_HOME_ID              = "admob_native_home_id"
    const val ADMOB_NATIVE_DOWNLOADS_ID         = "admob_native_downloads_id"

    // ── Ad behaviour ─────────────────────────────────────────────────────
    /** Minimum seconds between two interstitials (frequency cap). 0 = no cap. */
    const val INTERSTITIAL_MIN_INTERVAL_SEC = "interstitial_min_interval_sec"
    /** How long the "Loading ad…" dialog may block before we give up. */
    const val INTERSTITIAL_LOAD_TIMEOUT_MS  = "interstitial_load_timeout_ms"
    const val SPLASH_AD_DISPLAY_MS          = "splash_ad_display_ms"
    const val SPLASH_TIMEOUT_MS             = "splash_timeout_ms"
    const val SPLASH_NO_AD_DELAY_MS         = "splash_no_ad_delay_ms"
    const val ONBOARDING_AD_COUNTDOWN_SEC   = "onboarding_ad_countdown_sec"

    // ── Features / marketing ─────────────────────────────────────────────
    const val PRO_SCREEN_ENABLED    = "pro_screen_enabled"
    const val ANALYTICS_ENABLED     = "analytics_enabled"
    const val CRASHLYTICS_ENABLED   = "crashlytics_enabled"
    /** Comma-separated FCM topics every install subscribes to. */
    const val PUSH_TOPICS           = "push_topics"

    /**
     * In-app defaults. These are registered with `setDefaultsAsync` and also
     * back the initial [AppConfig] snapshot before Firebase has been touched.
     */
    val DEFAULTS: Map<String, Any> = mapOf(
        ADS_ENABLED to true,
        INTERSTITIAL_ADS_ENABLED to true,
        NATIVE_ADS_ENABLED to true,
        USE_TEST_ADS to false,

        AD_SPLASH_NATIVE_ENABLED to true,
        AD_APP_LAUNCH_INTERSTITIAL_ENABLED to true,
        AD_ONBOARDING_NATIVE_ENABLED to true,
        AD_GET_STARTED_INTERSTITIAL_ENABLED to true,
        AD_LANGUAGE_NATIVE_ENABLED to true,
        AD_HOME_NATIVE_ENABLED to true,
        AD_DOWNLOADS_NATIVE_ENABLED to true,
        AD_ON_DOWNLOAD_INTERSTITIAL_ENABLED to true,

        ADMOB_INTERSTITIAL_APP_LAUNCH_ID to AdManager.TEST_INTERSTITIAL_AD_UNIT_ID,
        ADMOB_INTERSTITIAL_GET_STARTED_ID to AdManager.TEST_INTERSTITIAL_AD_UNIT_ID,
        ADMOB_INTERSTITIAL_ON_DOWNLOAD_ID to AdManager.TEST_INTERSTITIAL_AD_UNIT_ID,
        ADMOB_NATIVE_SPLASH_ID to AdManager.TEST_NATIVE_AD_UNIT_ID,
        ADMOB_NATIVE_ONBOARDING_ID to AdManager.TEST_NATIVE_AD_UNIT_ID,
        ADMOB_NATIVE_LANGUAGE_ID to AdManager.TEST_NATIVE_AD_UNIT_ID,
        ADMOB_NATIVE_HOME_ID to AdManager.TEST_NATIVE_AD_UNIT_ID,
        ADMOB_NATIVE_DOWNLOADS_ID to AdManager.TEST_NATIVE_AD_UNIT_ID,

        INTERSTITIAL_MIN_INTERVAL_SEC to 30L,
        INTERSTITIAL_LOAD_TIMEOUT_MS to 4500L,
        SPLASH_AD_DISPLAY_MS to 4500L,
        SPLASH_TIMEOUT_MS to 7000L,
        SPLASH_NO_AD_DELAY_MS to 2000L,
        ONBOARDING_AD_COUNTDOWN_SEC to 5L,

        PRO_SCREEN_ENABLED to true,
        ANALYTICS_ENABLED to true,
        CRASHLYTICS_ENABLED to true,
        PUSH_TOPICS to "all_users,android"
    )
}
