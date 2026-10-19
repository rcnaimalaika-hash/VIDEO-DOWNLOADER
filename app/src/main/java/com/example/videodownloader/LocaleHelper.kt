package com.example.videodownloader

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    // Map language name → BCP-47 locale tag
    val languageToLocale = mapOf(
        "English"   to "en",
        "Urdu"      to "ur",
        "Hindi"     to "hi",
        "Arabic"    to "ar",
        "Bengali"   to "bn",
        "Punjabi"   to "pa",
        "Tamil"     to "ta",
        "Telugu"    to "te",
        "Malayalam" to "ml",
        "Japanese"  to "ja"
    )

    // RTL languages
    private val rtlLanguages = setOf("ur", "ar")

    fun isRtl(languageName: String): Boolean {
        val tag = languageToLocale[languageName] ?: "en"
        return tag in rtlLanguages
    }

    /**
     * Wraps the given Context with the selected locale.
     * Call this in Activity.attachBaseContext() via MainActivity.
     */
    fun wrapContext(context: Context, languageName: String): Context {
        val tag = languageToLocale[languageName] ?: "en"
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (tag in rtlLanguages) {
            config.setLayoutDirection(locale)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Returns a localized Configuration for the given language.
     */
    fun getLocalizedConfiguration(context: Context, languageName: String): Configuration {
        val tag = languageToLocale[languageName] ?: "en"
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (tag in rtlLanguages) {
            config.setLayoutDirection(locale)
        }
        return config
    }

    /**
     * Persist the selected language name to SharedPreferences.
     */
    fun saveLanguage(context: Context, languageName: String) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("selected_language", languageName)
            .apply()
    }

    /**
     * Read persisted language, defaults to "English".
     */
    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString("selected_language", "English") ?: "English"
    }
}
