package com.example.videodownloader.firebase

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.videodownloader.R
import java.net.HttpURLConnection
import java.net.URL

/**
 * Notification channels + rendering for pushes that arrive while the app is
 * in the foreground (background pushes with a `notification` block are drawn
 * by the system using the manifest meta-data defaults).
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    const val CHANNEL_GENERAL = "general_updates"      // must match manifest default_notification_channel_id
    const val CHANNEL_PROMO   = "offers_promotions"

    private val KNOWN_CHANNELS = setOf(CHANNEL_GENERAL, CHANNEL_PROMO)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val general = NotificationChannel(
            CHANNEL_GENERAL,
            context.getString(R.string.notification_channel_general),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.notification_channel_general_desc) }
        val promo = NotificationChannel(
            CHANNEL_PROMO,
            context.getString(R.string.notification_channel_promo),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.notification_channel_promo_desc) }
        nm.createNotificationChannels(listOf(general, promo))
    }

    fun canPostNotifications(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /**
     * Must be called off the main thread when [imageUrl] is set (it downloads the image).
     * FirebaseMessagingService.onMessageReceived already runs on a background thread.
     */
    @SuppressLint("MissingPermission")
    fun showPushNotification(
        context: Context,
        title: String,
        body: String,
        imageUrl: String?,
        data: Map<String, String>
    ) {
        if (!canPostNotifications(context)) {
            Log.w(TAG, "Notifications disabled / permission missing — dropping push")
            return
        }

        val channelId = data[PushIntents.EXTRA_CHANNEL]?.takeIf { it in KNOWN_CHANNELS } ?: CHANNEL_GENERAL
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            PushIntents.buildLaunchIntent(context, data),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_color))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        imageUrl?.let { loadBitmap(it) }?.let { bitmap ->
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(body)
            )
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun loadBitmap(url: String): Bitmap? = try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            instanceFollowRedirects = true
        }
        conn.inputStream.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        Log.w(TAG, "Could not load notification image: ${e.message}")
        null
    }
}
