package com.example.videodownloader.firebase

import android.content.Context
import android.util.Log
import com.example.videodownloader.BuildConfig
import com.example.videodownloader.ads.AdManager
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns the Firebase Remote Config lifecycle:
 *
 *  1. registers in-app defaults ([RemoteConfigKeys.DEFAULTS])
 *  2. activates whatever was fetched on a previous run (instant, offline-safe)
 *  3. fetches fresh values (`fetchAndActivate`)
 *  4. listens for *real-time* updates so a change published in the console
 *     reaches running apps within seconds — no restart needed
 *
 * Every consumer reads the typed [config] snapshot; nothing else in the app
 * should touch `FirebaseRemoteConfig` directly.
 */
object RemoteConfigManager {

    private const val TAG = "RemoteConfig"

    /** How long the splash / ad loaders wait for the very first fetch before proceeding with cached/default values. */
    const val FIRST_FETCH_WAIT_MS = 2_500L

    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    private val _config = MutableStateFlow(AppConfig.fromDefaults())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    /** Convenience accessor for non-Compose code. */
    val current: AppConfig get() = _config.value

    private val firstFetch = CompletableDeferred<Boolean>()
    private val initialized = AtomicBoolean(false)

    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (!initialized.compareAndSet(false, true)) return

        val settings = FirebaseRemoteConfigSettings.Builder()
            // Debug: fetch every time so you see console changes immediately.
            // Release: 1h is Firebase's recommended production interval (real-time
            // updates still arrive instantly through the ConfigUpdateListener).
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .setFetchTimeoutInSeconds(10)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)

        remoteConfig.setDefaultsAsync(RemoteConfigKeys.DEFAULTS).addOnCompleteListener {
            // Defaults + previously-activated values are readable now.
            publishSnapshot("defaults")
        }

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "fetchAndActivate ok (updated=${task.result})")
                } else {
                    Log.w(TAG, "fetchAndActivate failed: ${task.exception?.message}")
                }
                publishSnapshot("fetch")
                if (!firstFetch.isCompleted) firstFetch.complete(task.isSuccessful)
            }

        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                Log.d(TAG, "Real-time update for keys: ${configUpdate.updatedKeys}")
                remoteConfig.activate().addOnCompleteListener { publishSnapshot("realtime") }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.w(TAG, "Real-time listener error: ${error.message}")
            }
        })
    }

    /**
     * Suspends until the first fetch has finished (success or failure) or
     * [timeoutMs] elapses. Returns true only if fresh values were fetched.
     */
    suspend fun awaitFirstFetch(timeoutMs: Long = FIRST_FETCH_WAIT_MS): Boolean {
        if (!initialized.get()) return false
        return withTimeoutOrNull(timeoutMs) { firstFetch.await() } ?: false
    }

    /** Manual refresh (e.g. pull-to-refresh in a debug/settings screen). */
    fun refresh(onDone: ((Boolean) -> Unit)? = null) {
        if (!initialized.get()) { onDone?.invoke(false); return }
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            publishSnapshot("manual")
            onDone?.invoke(task.isSuccessful)
        }
    }

    private fun publishSnapshot(reason: String) {
        val snapshot = try {
            AppConfig.fromRemote(remoteConfig)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read remote config, keeping previous snapshot", e)
            return
        }
        val changed = snapshot != _config.value
        _config.value = snapshot
        Log.d(
            TAG, "Snapshot[$reason] ads=${snapshot.adsEnabled} interstitial=${snapshot.interstitialAdsEnabled} " +
                "native=${snapshot.nativeAdsEnabled} testAds=${snapshot.useTestAds} changed=$changed"
        )
        if (changed) {
            AdManager.onConfigChanged(snapshot)
            AnalyticsManager.onConfigChanged(snapshot)
            CrashReporter.onConfigChanged(snapshot)
            PushMessaging.onConfigChanged(snapshot)
        }
    }
}
