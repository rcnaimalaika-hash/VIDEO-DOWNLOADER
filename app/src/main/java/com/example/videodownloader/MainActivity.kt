package com.example.videodownloader

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.ui.theme.VIDEODOWNLOADERTheme

class MainActivity : ComponentActivity() {

    private val downloadViewModel: DownloadViewModel by viewModels()
    private val themeViewModel: ThemeViewModel     by viewModels()

    private var currentScreen by mutableStateOf("splash")
    private var previousScreen by mutableStateOf("home")

    fun navigateTo(destination: String) {
        previousScreen = currentScreen
        currentScreen = destination
    }

    fun navigateBack() {
        currentScreen = previousScreen
    }

    override fun attachBaseContext(newBase: Context) {
        val savedLang = LocaleHelper.getSavedLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, savedLang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads (AdMob) SDK & preloads
        AdManager.initialize(this)
        AdManager.registerActivity(this)

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

                    when (currentScreen) {

                        // Placement 1: Native Ad inside SplashScreen
                        // Placement 2: App Launching Interstitial Ad shown when splash completes
                        "splash" -> SplashScreen(
                            onFinished = {
                                AdManager.showInterstitial(
                                    activity = this@MainActivity,
                                    placementKey = AdManager.PLACEMENT_APP_LAUNCH,
                                    onDismissed = {
                                        val hasCompletedFirstLaunch = prefs.getBoolean("first_launch_completed", false)
                                        if (hasCompletedFirstLaunch) {
                                            currentScreen = "home"
                                        } else {
                                            currentScreen = "onboarding"
                                        }
                                    }
                                )
                            }
                        )

                        // Placement 3: Full-Screen Native Ad inside OnboardingScreen (First launch only)
                        // Placement 4: Get Started Interstitial Ad shown upon Get Started action
                        "onboarding" -> OnboardingScreen(
                            onGetStarted = {
                                AdManager.showInterstitial(
                                    activity = this@MainActivity,
                                    placementKey = AdManager.PLACEMENT_GET_STARTED,
                                    onDismissed = {
                                        currentScreen = "language"
                                    }
                                )
                            }
                        )

                        // Placement 5: Large Native Ad inside LanguageScreen
                        "language" -> LanguageScreen(
                            onContinue = { selectedLanguage ->
                                LocaleHelper.saveLanguage(this@MainActivity, selectedLanguage.name)
                                currentLanguage = selectedLanguage.name
                                prefs.edit()
                                    .putBoolean("language_chosen", true)
                                    .putBoolean("first_launch_completed", true)
                                    .apply()
                                if (previousScreen != "splash" && previousScreen != "onboarding") {
                                    navigateBack()
                                } else {
                                    currentScreen = "home"
                                }
                            }
                        )

                    "home" -> HomeScreen(
                        viewModel        = downloadViewModel,
                        themeViewModel   = themeViewModel,
                        onMenuClick      = {},
                        onViewAll        = { navigateTo("downloads") },
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
                        onTabSelected    = { if (it == 0) currentScreen = "home" },
                        onHomeClick      = { currentScreen = "home" },
                        onLanguageClick  = { navigateTo("language") },
                        onSettingsClick  = { navigateTo("settings") },
                        onRateClick      = { navigateTo("rate") },
                        onShareClick     = { navigateTo("share") },
                        onHelpClick      = { navigateTo("help") },
                        onPrivacyClick   = { navigateTo("privacy") },
                        onAboutClick     = { navigateTo("about") }
                    )

                    "settings" -> SettingsScreen(
                        onHomeClick      = { currentScreen = "home" },
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
