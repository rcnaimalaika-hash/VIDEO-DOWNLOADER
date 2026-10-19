package com.example.videodownloader

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Download API Configuration & Endpoints
// Server: http://110.93.223.194:5693
// ---------------------------------------------------------------------------

data class SupportedPlatform(
    val id: String,
    val name: String,
    val domain: String,
    val iconEmoji: String = "🎬"
)

object DownloadApi {
    const val BASE_URL = "http://110.93.223.194:5693"
    const val PROCESS1_URL = "$BASE_URL/process1"
    const val PROCESS2_URL = "$BASE_URL/process2"
    const val PLATFORMS_URL = "$BASE_URL/api/platforms"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    /** Single client for both the POST request and the streaming file download */
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)   // 0 = infinite for streaming large video files
            .writeTimeout(45, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** Default fallback list of supported platforms */
    val defaultPlatforms = listOf(
        SupportedPlatform("tiktok", "TikTok", "tiktok.com", "🎵"),
        SupportedPlatform("instagram", "Instagram", "instagram.com", "📸"),
        SupportedPlatform("facebook", "Facebook", "facebook.com", "📘"),
        SupportedPlatform("twitter", "X / Twitter", "x.com", "🐦"),
        SupportedPlatform("pinterest", "Pinterest", "pinterest.com", "📌"),
        SupportedPlatform("reddit", "Reddit", "reddit.com", "🤖"),
        SupportedPlatform("snapchat", "Snapchat", "snapchat.com", "👻"),
        SupportedPlatform("threads", "Threads", "threads.com", "🧵"),
        SupportedPlatform("dailymotion", "Dailymotion", "dailymotion.com", "▶️"),
        SupportedPlatform("telegram", "Telegram", "t.me", "✈️"),
        SupportedPlatform("spotify", "Spotify", "spotify.com", "🎧")
    )

    /** Fetch supported platforms dynamically from the server */
    fun fetchPlatforms(): List<SupportedPlatform> {
        return try {
            val req = Request.Builder().url(PLATFORMS_URL).get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return defaultPlatforms
            val body = resp.body?.string() ?: return defaultPlatforms
            val json = JSONObject(body)
            val arr = json.optJSONArray("platforms") ?: return defaultPlatforms
            val result = mutableListOf<SupportedPlatform>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val platformName = item.optString("platform", "")
                val domain = item.optString("domain", "")
                val emoji = when (platformName.lowercase()) {
                    "tiktok" -> "🎵"
                    "instagram" -> "📸"
                    "facebook" -> "📘"
                    "twitter" -> "🐦"
                    "pinterest" -> "📌"
                    "reddit" -> "🤖"
                    "snapchat" -> "👻"
                    "threads" -> "🧵"
                    "spotify", "open.spotify" -> "🎧"
                    "telegram" -> "✈️"
                    "dailymotion" -> "▶️"
                    else -> "🎬"
                }
                if (platformName.isNotBlank() && result.none { it.name.equals(platformName, true) }) {
                    result.add(
                        SupportedPlatform(
                            id = platformName.lowercase(),
                            name = platformName.replaceFirstChar { it.uppercase() },
                            domain = domain,
                            iconEmoji = emoji
                        )
                    )
                }
            }
            if (result.isNotEmpty()) result else defaultPlatforms
        } catch (e: Exception) {
            defaultPlatforms
        }
    }
}
