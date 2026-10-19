package com.example.videodownloader

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videodownloader.ui.theme.VIDEODOWNLOADERTheme

class MainActivity : ComponentActivity() {

    private val downloadViewModel: DownloadViewModel by viewModels()
    private val themeViewModel: ThemeViewModel     by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val savedLang = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, savedLang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        setContent {
            // Observe dark mode — theme recomposes instantly when toggled
            val isDark by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
            var currentLanguage by remember { mutableStateOf(LocaleHelper.getSavedLanguage(this@MainActivity)) }

            val localizedContext = remember(currentLanguage) {
                LocaleHelper.wrapContext(this@MainActivity, currentLanguage)
            }
            val localizedConfig = remember(currentLanguage) {
                LocaleHelper.getLocalizedConfiguration(this@MainActivity, currentLanguage)
            }
            val layoutDirection = if (LocaleHelper.isRtl(currentLanguage)) {
                androidx.compose.ui.unit.LayoutDirection.Rtl
            } else {
                androidx.compose.ui.unit.LayoutDirection.Ltr
            }

            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localizedContext,
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfig,
                androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
            ) {
                VIDEODOWNLOADERTheme(darkTheme = isDark, dynamicColor = false) {

                    var screen by remember { mutableStateOf("splash") }
                    var previousScreen by remember { mutableStateOf("home") }

                    fun navigateTo(destination: String) {
                        previousScreen = screen
                        screen = destination
                    }

                    fun navigateBack() {
                        screen = previousScreen
                    }

                    when (screen) {

                        "splash" -> SplashScreen(
                            onFinished = { screen = "onboarding" }
                        )

                        "onboarding" -> OnboardingScreen(
                            onGetStarted = { screen = "language" }
                        )

                        "language" -> LanguageScreen(
                            onContinue = { selectedLanguage ->
                                LocaleHelper.saveLanguage(this@MainActivity, selectedLanguage.name)
                                currentLanguage = selectedLanguage.name
                                prefs.edit().putBoolean("language_chosen", true).apply()
                                screen = "home"
                            }
                        )

                    "home" -> HomeScreen(
                        viewModel        = downloadViewModel,
                        themeViewModel   = themeViewModel,
                        onMenuClick      = {},
                        onViewAll        = { navigateTo("downloads") },
                        onProClick       = { navigateTo("pro") },
                        onBookmarkClick  = { navigateTo("bookmark") },
                        onLanguageClick  = { navigateTo("language") },
                        onDownloadsClick = { navigateTo("downloads") },
                        onSettingsClick  = { navigateTo("settings") },
                        onShareClick     = { navigateTo("share") },
                        onRateClick      = { navigateTo("rate") },
                        onHelpClick      = { navigateTo("help") },
                        onPrivacyClick   = { navigateTo("privacy") },
                        onAboutClick     = { navigateTo("about") }
                    )

                    "rate" -> RateAppScreen(
                        onBack          = { navigateBack() },
                        onMaybeLater    = { navigateBack() },
                        onRateSubmitted = { navigateBack() }
                    )

                    "downloads" -> DownloadScreen(
                        viewModel        = downloadViewModel,
                        themeViewModel   = themeViewModel,
                        onTabSelected    = { if (it == 0) screen = "home" },
                        onHomeClick      = { screen = "home" },
                        onProClick       = { navigateTo("pro") },
                        onLanguageClick  = { navigateTo("language") },
                        onSettingsClick  = { navigateTo("settings") },
                        onRateClick      = { navigateTo("rate") },
                        onShareClick     = { navigateTo("share") },
                        onHelpClick      = { navigateTo("help") },
                        onPrivacyClick   = { navigateTo("privacy") },
                        onAboutClick     = { navigateTo("about") }
                    )

                    "settings" -> SettingsScreen(
                        onHomeClick      = { screen = "home" },
                        onDownloadsClick = { navigateTo("downloads") },
                        onRateClick      = { navigateTo("rate") },
                        onPrivacyClick   = { navigateTo("privacy") },
                        onAboutClick     = { navigateTo("about") },
                        onHelpClick      = { navigateTo("help") },
                        onBack           = { navigateBack() }
                    )

                    "help" -> HelpSupportScreen(
                        onBack = { navigateBack() }
                    )

                    "privacy" -> PrivacyPolicyScreen(
                        onBack = { navigateBack() }
                    )

                    "about" -> AboutAppScreen(
                        onBack = { navigateBack() }
                    )

                    "share" -> ShareAppScreen(
                        onBack = { navigateBack() }
                    )

                    "pro" -> ProScreen(
                        onBack      = { navigateBack() },
                        onGoPremium = { navigateBack() }
                    )

                    "bookmark" -> BookmarkScreen(
                        onBack = { navigateBack() }
                    )
                }
            }
        }
    }
}
}
