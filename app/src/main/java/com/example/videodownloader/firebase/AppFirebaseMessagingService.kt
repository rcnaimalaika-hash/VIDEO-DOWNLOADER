package com.example.videodownloader.firebase

import android.util.Log
import com.example.videodownloader.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives FCM messages while the app is in the foreground (and *all*
 * data-only messages), plus registration-token refreshes.
 *
 * Payload contract: see [PushIntents].
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    // ── Registration callbacks ───────────────────────────────────────────
    // FCM 25.1+ registers the app instance with its Firebase installation ID
    // (enabled via `firebase_messaging_installation_id_enabled` in the manifest);
    // the registration token is delivered here instead of onNewToken().
    override fun onRegistered(token: String) {
        super.onRegistered(token)
        PushMessaging.onNewToken(token)
    }

    override fun onUnregistered(token: String) {
        super.onUnregistered(token)
        PushMessaging.onUnregistered(token)
    }

    /** Legacy token callback — only invoked when FID registration is disabled in the manifest. */
    @Deprecated("Legacy FCM token API; onRegistered() is used when FID registration is enabled")
    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushMessaging.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        Log.d(TAG, "Message from=${message.from} id=${message.messageId} data=$data notification=${message.notification != null}")

        AnalyticsManager.logPushReceived(data[PushIntents.EXTRA_CAMPAIGN], message.messageId)

        // Silent actions (no notification shown)
        when (data[PushIntents.EXTRA_ACTION]) {
            PushIntents.ACTION_REFRESH_CONFIG -> {
                Log.d(TAG, "Silent push: refreshing Remote Config")
                RemoteConfigManager.refresh()
                return
            }
        }

        val title = message.notification?.title ?: data[PushIntents.EXTRA_TITLE]
        val body  = message.notification?.body  ?: data[PushIntents.EXTRA_BODY]
        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Log.d(TAG, "Data-only message without title/body — nothing to display")
            return
        }

        val imageUrl = message.notification?.imageUrl?.toString() ?: data[PushIntents.EXTRA_IMAGE]

        NotificationHelper.showPushNotification(
            context  = this,
            title    = title?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name),
            body     = body ?: "",
            imageUrl = imageUrl,
            data     = data
        )
    }
}
