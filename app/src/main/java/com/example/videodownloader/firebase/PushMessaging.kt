package com.example.videodownloader.firebase

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.videodownloader.LocaleHelper
import com.google.firebase.messaging.FirebaseMessaging

/**
 * FCM registration-token + topic bookkeeping.
 *
 * Topics give marketing a free, server-less way to target segments from the
 * Firebase console ("Send to topic"):
 *   - the list in Remote Config `push_topics` (default: all_users, android)
 *   - `lang_<code>` for the language the user picked (localised campaigns)
 *
 * If you later add a backend, send the token from [onNewToken] to it.
 */
object PushMessaging {

    private const val TAG = "PushMessaging"
    private const val PREFS = "fcm_prefs"
    private const val KEY_TOKEN = "fcm_token"
    private const val KEY_CONFIG_TOPICS = "config_topics"
    private const val KEY_LANGUAGE_TOPIC = "language_topic"

    /**
     * Manifest meta-data that switches the FCM SDK (25.1+) to Firebase-installation-ID
     * based registration: `register()` + `onRegistered()` replace the deprecated
     * `getToken()` + `onNewToken()`. Both paths are supported here; the flag decides.
     */
    private const val META_FID_REGISTRATION = "firebase_messaging_installation_id_enabled"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext

        registerForPush(context.applicationContext)

        syncConfigTopics(RemoteConfigManager.current.pushTopics)
        updateLanguageTopic(LocaleHelper.getSavedLanguage(context))
    }

    /** true when the manifest enables the FCM 25.1+ FID-based registration API. */
    fun isFidRegistrationEnabled(context: Context): Boolean = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        info.metaData?.getBoolean(META_FID_REGISTRATION, false) ?: false
    } catch (e: Exception) {
        false
    }

    private fun registerForPush(context: Context) {
        val messaging = FirebaseMessaging.getInstance()
        if (isFidRegistrationEnabled(context)) {
            // Token is delivered asynchronously to AppFirebaseMessagingService.onRegistered()
            messaging.register().addOnCompleteListener { task ->
                if (task.isSuccessful) Log.d(TAG, "FCM registration requested (token arrives via onRegistered)")
                else Log.w(TAG, "FCM register() failed", task.exception)
            }
        } else {
            fetchLegacyToken(messaging)
        }
    }

    /** Pre-25.1 token API, used only when FID registration is disabled in the manifest. */
    @Suppress("DEPRECATION")
    private fun fetchLegacyToken(messaging: FirebaseMessaging) {
        messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) onNewToken(task.result) else Log.w(TAG, "Fetching FCM token failed", task.exception)
        }
    }

    /** Called by the messaging service (onRegistered / legacy onNewToken) and by [fetchLegacyToken]. */
    fun onNewToken(token: String) {
        Log.d(TAG, "FCM token: $token")
        prefs()?.edit()?.putString(KEY_TOKEN, token)?.apply()
        // TODO(backend): POST the token to your server here if you need per-device targeting.
    }

    /** Called when the SDK reports the registration was removed (unregister()). */
    fun onUnregistered(token: String) {
        Log.d(TAG, "FCM unregistered: $token")
        prefs()?.edit()?.remove(KEY_TOKEN)?.apply()
    }

    fun getToken(): String? = prefs()?.getString(KEY_TOKEN, null)

    internal fun onConfigChanged(config: AppConfig) = syncConfigTopics(config.pushTopics)

    /** Keeps the device subscribed to exactly the topics listed in Remote Config. */
    fun syncConfigTopics(desired: List<String>) {
        val p = prefs() ?: return
        val previous = p.getStringSet(KEY_CONFIG_TOPICS, emptySet()) ?: emptySet()
        val wanted = desired.toSet()
        (wanted - previous).forEach { subscribe(it) }
        (previous - wanted).forEach { unsubscribe(it) }
        p.edit().putStringSet(KEY_CONFIG_TOPICS, wanted).apply()
    }

    /** Subscribes to `lang_<code>` for [languageName] and drops the previous language topic. */
    fun updateLanguageTopic(languageName: String) {
        val p = prefs() ?: return
        val tag = LocaleHelper.languageToLocale[languageName] ?: "en"
        val topic = "lang_$tag"
        val previous = p.getString(KEY_LANGUAGE_TOPIC, null)
        if (previous == topic) return
        previous?.let { unsubscribe(it) }
        subscribe(topic)
        p.edit().putString(KEY_LANGUAGE_TOPIC, topic).apply()
    }

    fun subscribe(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener {
            Log.d(TAG, "subscribe($topic) success=${it.isSuccessful}")
        }
    }

    fun unsubscribe(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener {
            Log.d(TAG, "unsubscribe($topic) success=${it.isSuccessful}")
        }
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
