package com.example.videodownloader

import android.app.Application
import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.firebase.AnalyticsManager
import com.example.videodownloader.firebase.CrashReporter
import com.example.videodownloader.firebase.NotificationHelper
import com.example.videodownloader.firebase.PushMessaging
import com.example.videodownloader.firebase.RemoteConfigManager
import com.google.firebase.FirebaseApp

/**
 * Process-wide bootstrap. Order matters:
 *   Firebase → Crashlytics → Remote Config (defaults) → Analytics → Push → Ads
 * so that every later component can read the Remote Config snapshot.
 */
class VideoDownloaderApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        CrashReporter.init(this)
        RemoteConfigManager.init(this)
        AnalyticsManager.init(this)

        NotificationHelper.createChannels(this)
        PushMessaging.init(this)

        // AdMob SDK + interstitial preloads (gated by Remote Config inside AdManager)
        AdManager.initialize(this)
    }
}
