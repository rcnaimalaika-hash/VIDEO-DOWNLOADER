package com.example.videodownloader.firebase

import android.content.Context
import com.example.videodownloader.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Thin wrapper around Firebase Crashlytics. Collection is switched from
 * Remote Config (`crashlytics_enabled`) and disabled in debug builds so local
 * development crashes don't pollute the dashboard.
 */
object CrashReporter {

    /** Flip to true when you want to verify the Crashlytics integration from a debug build. */
    private const val REPORT_IN_DEBUG = false

    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        apply(RemoteConfigManager.current)
        crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
    }

    internal fun onConfigChanged(config: AppConfig) = apply(config)

    private fun apply(config: AppConfig) {
        val enabled = config.crashlyticsEnabled && (!BuildConfig.DEBUG || REPORT_IN_DEBUG)
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    /** Breadcrumb that shows up in the crash report timeline. */
    fun log(message: String) = crashlytics.log(message)

    /** Non-fatal error (appears under "Non-fatals" in the console). */
    fun recordException(throwable: Throwable) = crashlytics.recordException(throwable)

    fun setKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

    fun setScreen(screen: String) = crashlytics.setCustomKey("current_screen", screen)

    fun setUserId(id: String) = crashlytics.setUserId(id)
}
