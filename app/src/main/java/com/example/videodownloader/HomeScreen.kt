package com.example.videodownloader

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.ads.LargeNativeAdCard
import com.example.videodownloader.firebase.RemoteConfigManager
import com.example.videodownloader.ui.theme.AppColors
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Home Screen
// ---------------------------------------------------------------------------

@Composable
fun HomeScreen(
    viewModel: DownloadViewModel,
    themeViewModel: ThemeViewModel,
    onMenuClick: () -> Unit = {},
    onViewAll: () -> Unit = {},
    onTabSelected: (Int) -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRateClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onProClick: () -> Unit = {},
    userEmail: String? = null,
    onLogoutClick: () -> Unit = {}
) {
    val context            = LocalContext.current
    val lifecycleOwner     = LocalLifecycleOwner.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager       = androidx.compose.ui.platform.LocalFocusManager.current

    val inputState      by viewModel.inputState.collectAsState()
    val flowStep        by viewModel.flowStep.collectAsState()
    val activeFlowJob   by viewModel.activeFlowJob.collectAsState()
    val isDarkMode      by themeViewModel.isDarkMode.collectAsStateWithLifecycle()

    var selectedTab     by remember { mutableStateOf(0) }
    var drawerItem      by remember { mutableStateOf("Home") }
    val drawerState     = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope           = rememberCoroutineScope()

    // Dismiss keyboard whenever drawer opens
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }

    // ── Auto-paste from clipboard whenever screen comes to foreground ─────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.autoPasteFromClipboard(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Show status snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(inputState.errorMessage) {
        inputState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }
    LaunchedEffect(inputState.successMessage) {
        inputState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSuccess()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                selectedItem     = drawerItem,
                isDarkMode       = isDarkMode,
                userEmail        = userEmail,
                onItemClick  = { item ->
                    drawerItem = item
                    scope.launch { drawerState.close() }
                    when (item) {
                        "Downloads"       -> onDownloadsClick()
                        "Language"        -> onLanguageClick()
                        "Settings"        -> onSettingsClick()
                        "Rate Us"         -> onRateClick()
                        "Share App"       -> onShareClick()
                        "Help & Support"  -> onHelpClick()
                        "Privacy Policy"  -> onPrivacyClick()
                        "About App"       -> onAboutClick()
                        "Log Out"         -> onLogoutClick()
                    }
                },
                onDarkModeToggle = { themeViewModel.setDarkMode(it) },
                onGoPremium = {
                    scope.launch { drawerState.close() }
                    // Feature flag: `pro_screen_enabled` in Remote Config
                    if (RemoteConfigManager.current.proScreenEnabled) onProClick()
                }
            )
        },
        scrimColor = Color(0x99000000)
    ) {
        Scaffold(
            containerColor = AppColors.background,
            snackbarHost   = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AppBottomNavBar(
                    selectedIndex = selectedTab,
                    onTabSelected = {
                        selectedTab = it
                        onTabSelected(it)
                        if (it == 1) onDownloadsClick()
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .verticalScroll(rememberScrollState())
                ) {
                    HeaderBanner(
                        onMenuClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            scope.launch { drawerState.open() }
                        }
                    )

                    val activity = context as? Activity

                    // ── Paste Link & Download Card ───────────────────────────
                    PasteLinkCard(
                        urlText          = inputState.url,
                        isLoading        = inputState.isLoading,
                        selectedFormat   = inputState.format,
                        selectedFileType = inputState.fileType,
                        onUrlChange      = viewModel::onUrlChange,
                        onFormatChange   = viewModel::onFormatChange,
                        onFileTypeChange = viewModel::onFileTypeChange,
                        onPaste          = { viewModel.pasteFromClipboard(context) },
                        onDownload       = {
                            Log.d("AdManager", "🚀 [HomeScreen] PasteLinkCard 'Download' clicked -> calling viewModel.startDownload()")
                            viewModel.startDownload()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Placement 6: Home Screen Large Native Ad
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        LargeNativeAdCard(placement = AdManager.NATIVE_HOME)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // ── Download Flow Modal Sheets (Format Selection, Progress, Complete) ──
                DownloadFlowModal(
                    flowStep = flowStep,
                    job = activeFlowJob,
                    onFormatConfirmed = { fileType, format ->
                        Log.d("AdManager", "🎯 [HomeScreen] onFormatConfirmed received for type=$fileType, format=$format -> calling AdManager.showInterstitial")
                        AdManager.showInterstitial(
                            context = context,
                            placementKey = AdManager.PLACEMENT_ON_DOWNLOAD,
                            onDismissed = {
                                Log.d("AdManager", "🎬 [HomeScreen] Ad dismissed -> calling confirmFormatAndDownload($fileType, $format)")
                                viewModel.confirmFormatAndDownload(fileType, format)
                            }
                        )
                    },
                    onCancelDownload = {
                        viewModel.cancelActiveFlow()
                    },
                    onViewInDownloads = {
                        viewModel.dismissFlow()
                        onDownloadsClick()
                    },
                    onDismiss = {
                        viewModel.dismissFlow()
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header Banner
// ---------------------------------------------------------------------------

@Composable
private fun HeaderBanner(onMenuClick: () -> Unit) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 160.dp else 220.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A6BFF), Color(0xFF1252CC))))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isLandscape) 28.dp else 40.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, AppColors.background)),
                    RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.home_welcome),
                        fontSize = if (isLandscape) 18.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 6.dp))
                    Text(
                        stringResource(R.string.home_subtitle),
                        fontSize = if (isLandscape) 12.sp else 13.sp,
                        color = Color(0xCCFFFFFF),
                        lineHeight = if (isLandscape) 16.sp else 18.sp
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.welcome_screen_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(if (isLandscape) 80.dp else 120.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Paste Link & Download Card
// ---------------------------------------------------------------------------

@Composable
private fun PasteLinkCard(
    urlText: String,
    isLoading: Boolean,
    selectedFormat: String,
    selectedFileType: String,
    onUrlChange: (String) -> Unit,
    onFormatChange: (String) -> Unit,
    onFileTypeChange: (String) -> Unit,
    onPaste: () -> Unit,
    onDownload: () -> Unit
) {
    val isDark = AppColors.isDark
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-20).dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isDark) Color(0x66000000) else Color(0x1F1E40AF),
                spotColor = if (isDark) Color(0x88000000) else Color(0x331E40AF)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            // Title + paste button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.home_paste_title),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Bookmark button (saves URL to SQLite Database)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.surface2)
                            .border(1.dp, AppColors.divider, RoundedCornerShape(10.dp))
                            .clickable {
                                if (urlText.isNotBlank()) {
                                    val db = com.example.videodownloader.db.AppDatabaseHelper.getInstance(context)
                                    val title = urlText.substringAfterLast("/").substringBefore("?").takeIf { it.length > 2 } ?: "Saved Video Link"
                                    val inserted = db.addBookmark(
                                        com.example.videodownloader.db.BookmarkEntity(
                                            title = title,
                                            url = urlText.trim(),
                                            format = "MP4"
                                        )
                                    )
                                    if (inserted > 0) {
                                        android.widget.Toast.makeText(context, "Saved to SQLite Bookmarks!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "Bookmark already saved or error", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Please enter a URL to bookmark", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "Bookmark", tint = AppColors.blue, modifier = Modifier.size(18.dp))
                    }

                    // Paste button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.surface2)
                            .border(1.dp, AppColors.divider, RoundedCornerShape(10.dp))
                            .clickable { onPaste() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", tint = AppColors.blue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // URL text field
            OutlinedTextField(
                value = urlText,
                onValueChange = onUrlChange,
                placeholder = {
                    Text(stringResource(R.string.home_url_placeholder), color = AppColors.textSecondary, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Link, null, tint = AppColors.textSecondary, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor    = AppColors.divider,
                    focusedBorderColor      = AppColors.blue,
                    unfocusedContainerColor = AppColors.surface2,
                    focusedContainerColor   = AppColors.surface,
                    unfocusedTextColor      = AppColors.textPrimary,
                    focusedTextColor        = AppColors.textPrimary,
                    cursorColor             = AppColors.blue,
                    focusedPlaceholderColor = AppColors.textSecondary,
                    unfocusedPlaceholderColor = AppColors.textSecondary,
                    focusedLeadingIconColor = AppColors.textSecondary,
                    unfocusedLeadingIconColor = AppColors.textSecondary
                ),
                singleLine = true,
                textStyle  = LocalTextStyle.current.copy(fontSize = 13.sp),
                enabled    = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Download button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isLoading) Color(0xFF93B4F5) else AppColors.blue)
                    .clickable(enabled = !isLoading) { onDownload() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Starting download…",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_download),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun HomeScreenPreview() {
    // Preview placeholder
}


