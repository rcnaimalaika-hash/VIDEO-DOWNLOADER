package com.example.videodownloader.firebase

import android.content.Context
import android.content.Intent
import com.example.videodownloader.MainActivity

/**
 * Contract between push payloads and in-app navigation.
 *
 * Supported `data` keys on an FCM message:
 *   screen    one of [VALID_SCREENS] — opened after splash/onboarding
 *   url       external link opened in the browser (e.g. Play Store listing, promo page)
 *   campaign  free-text label surfaced in analytics (`push_received` / `push_opened`)
 *   channel   notification channel id (see [NotificationHelper]); defaults to general
 *   image     https image url for a big-picture notification (foreground only)
 *   title/body used when the message has no `notification` block (data-only message)
 *   action    "refresh_config" → silently re-fetches Remote Config, no notification shown
 *
 * Background notifications rendered by the system tray forward the same data
 * keys as Intent extras, so [extract] works for both foreground and background.
 */
object PushIntents {

    const val EXTRA_SCREEN    = "screen"
    const val EXTRA_URL       = "url"
    const val EXTRA_CAMPAIGN  = "campaign"
    const val EXTRA_CHANNEL   = "channel"
    const val EXTRA_IMAGE     = "image"
    const val EXTRA_TITLE     = "title"
    const val EXTRA_BODY      = "body"
    const val EXTRA_ACTION    = "action"
    const val EXTRA_FROM_PUSH = "from_push"

    const val ACTION_REFRESH_CONFIG = "refresh_config"

    /** Extra FCM attaches to tray-rendered notification taps. */
    private const val FCM_MESSAGE_ID_EXTRA = "google.message_id"

    val VALID_SCREENS = setOf(
        "home", "downloads", "settings", "pro", "rate", "share",
        "help", "privacy", "about", "language", "bookmark"
    )

    data class PushDeepLink(
        val screen: String?,
        val url: String?,
        val campaign: String?
    )

    fun buildLaunchIntent(context: Context, data: Map<String, String>): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_PUSH, true)
            data.forEach { (k, v) -> putExtra(k, v) }
        }

    /** Returns null when the intent did not originate from a push notification. */
    fun extract(intent: Intent?): PushDeepLink? {
        val extras = intent?.extras ?: return null
        val fromPush = extras.getBoolean(EXTRA_FROM_PUSH, false) || extras.containsKey(FCM_MESSAGE_ID_EXTRA)
        val screen = extras.getString(EXTRA_SCREEN)?.trim()?.lowercase()
        val url = extras.getString(EXTRA_URL)?.trim()
        if (!fromPush && screen == null && url == null) return null
        return PushDeepLink(
            screen = screen?.takeIf { it in VALID_SCREENS },
            url = url?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
            campaign = extras.getString(EXTRA_CAMPAIGN)
        )
    }

    /** Removes push extras so a config change (rotation) doesn't re-trigger navigation. */
    fun clear(intent: Intent?) {
        intent ?: return
        listOf(EXTRA_FROM_PUSH, EXTRA_SCREEN, EXTRA_URL, EXTRA_CAMPAIGN, FCM_MESSAGE_ID_EXTRA)
            .forEach { intent.removeExtra(it) }
    }
}
