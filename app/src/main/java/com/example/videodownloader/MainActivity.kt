package com.example.videodownloader

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.firebase.AnalyticsManager
import com.example.videodownloader.firebase.PushIntents
import com.example.videodownloader.firebase.PushMessaging
import com.example.videodownloader.ui.theme.VIDEODOWNLOADERTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREF_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

        /** Screens that are part of the launch flow — push deep-links wait until Home. */
        private val LAUNCH_FLOW_SCREENS = setOf("splash", "onboarding", "language")
    }

    private val downloadViewModel: DownloadViewModel by viewModels()
    private val themeViewModel: ThemeViewModel     by viewModels()

    private var currentScreen by mutableStateOf("splash")
    private var previousScreen by mutableStateOf("home")

    /** Screen requested by a push-notification tap, applied once the app reaches Home. */
    private var pendingPushScreen by mutableStateOf<String?>(null)

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

        // AdMob / Firebase are initialised in VideoDownloaderApp; just register the Activity for interstitials
        AdManager.registerActivity(this)

        // Push notification tap (cold start)
        handlePushIntent(intent)

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

            // ── Analytics: one screen_view per in-app destination (no Navigation lib → manual) ──
            LaunchedEffect(currentScreen) {
                AnalyticsManager.logScreenView(currentScreen)
            }

            // ── Push deep-link: navigate once the launch flow is done ──
            LaunchedEffect(currentScreen, pendingPushScreen) {
                val target = pendingPushScreen ?: return@LaunchedEffect
                if (currentScreen == "home") {
                    pendingPushScreen = null
                    if (target != "home") navigateTo(target)
                }
            }

            // ── Android 13+ notification permission: asked once, after the user reaches Home ──
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                AnalyticsManager.logNotificationPermission(granted)
            }
            LaunchedEffect(currentScreen) {
                if (currentScreen == "home" &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                    !prefs.getBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, false)
                ) {
                    prefs.edit().putBoolean(PREF_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
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
                                AnalyticsManager.logLanguageSelected(selectedLanguage.name)
                                PushMessaging.updateLanguageTopic(selectedLanguage.name)
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
                        onAboutClick     = { navigateTo("about") },
                        onProClick       = { navigateTo("pro") }
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

    /** Push notification tap while the Activity is already running (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    /**
     * Applies the `screen` / `url` / `campaign` data of a push notification.
     * See [PushIntents] for the payload contract.
     */
    private fun handlePushIntent(intent: Intent?) {
        val link = PushIntents.extract(intent) ?: return
        PushIntents.clear(intent)
        Log.d(TAG, "Push deep-link: $link")
        AnalyticsManager.logPushOpened(link.campaign, link.screen)

        link.url?.let { url ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Log.w(TAG, "Could not open push url $url: ${e.message}")
            }
        }

        link.screen?.let { screen ->
            if (currentScreen in LAUNCH_FLOW_SCREENS) {
                pendingPushScreen = screen
            } else if (screen != currentScreen) {
                navigateTo(screen)
            }
        }
    }
}
