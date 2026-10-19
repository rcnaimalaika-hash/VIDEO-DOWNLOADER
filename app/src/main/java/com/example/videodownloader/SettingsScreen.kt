package com.example.videodownloader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors
import com.example.videodownloader.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Settings Screen (Matching Settings.png mockup)
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    onHomeClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onRateClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedQuality by remember { mutableStateOf("1080p") }
    var showQualityDialog by remember { mutableStateOf(false) }

    var wifiOnly by remember { mutableStateOf(true) }
    var autoDownload by remember { mutableStateOf(false) }
    var cacheSize by remember { mutableStateOf("120 MB") }

    var showLocationDialog by remember { mutableStateOf(false) }

    val iconBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)
    val iconTint = Color(0xFF2563EB)

    Scaffold(
        containerColor = AppColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavBar(
                selectedIndex = -1,
                onTabSelected = { tab ->
                    if (tab == 0) onHomeClick()
                    else if (tab == 1) onDownloadsClick()
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            // ── Screen Title ──────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── 1. Download Location ──────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.Folder,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_download_location),
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.settings_internal_storage),
                            fontSize = 14.sp,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                onClick = { showLocationDialog = true }
            )

            // ── 2. Video Quality ──────────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.Settings,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_video_quality),
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedQuality,
                            fontSize = 14.sp,
                            color = AppColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = AppColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                onClick = { showQualityDialog = true }
            )

            // ── 3. Download with Wi-Fi only ───────────────────────────────
            SettingsToggleItem(
                icon = Icons.Outlined.Wifi,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_wifi_only),
                isChecked = wifiOnly,
                onCheckedChange = { wifiOnly = it }
            )

            // ── 4. Auto Download ──────────────────────────────────────────
            SettingsToggleItem(
                icon = Icons.Outlined.Sync,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_auto_download),
                isChecked = autoDownload,
                onCheckedChange = { autoDownload = it }
            )

            // ── 5. Clear Cache ────────────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.Delete,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_clear_cache),
                trailingContent = {
                    Text(
                        text = cacheSize,
                        fontSize = 14.sp,
                        color = AppColors.textSecondary
                    )
                },
                onClick = {
                    if (cacheSize != "0 MB") {
                        cacheSize = "0 MB"
                        scope.launch {
                            snackbarHostState.showSnackbar("Cache cleared successfully!")
                        }
                    }
                }
            )

            // ── Divider ───────────────────────────────────────────────────
            Divider(
                color = AppColors.divider,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            // ── 6. Help & Support ────────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.HelpOutline,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.help_title),
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onHelpClick
            )

            // ── 7. Rate Us ────────────────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.StarOutline,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_rate_us),
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onRateClick
            )

            // ── 8. Privacy Policy ─────────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.Shield,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_privacy_policy),
                trailingContent = {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = onPrivacyClick
            )

            // ── 9. About App ──────────────────────────────────────────────
            SettingsClickableItem(
                icon = Icons.Outlined.Info,
                iconBg = iconBgColor,
                iconTint = iconTint,
                title = stringResource(R.string.settings_about_app),
                trailingContent = {
                    Text(
                        text = "v1.0.0",
                        fontSize = 14.sp,
                        color = AppColors.textSecondary
                    )
                },
                onClick = onAboutClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    // Quality Selection Dialog
    if (showQualityDialog) {
        val qualities = listOf("1080p", "720p", "480p", "Best Available")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text(text = stringResource(R.string.settings_video_quality), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    qualities.forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedQuality = q
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (q == selectedQuality),
                                onClick = {
                                    selectedQuality = q
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = q, fontSize = 15.sp, color = AppColors.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Cancel", color = Color(0xFF2563EB))
                }
            },
            containerColor = AppColors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Download Location Dialog
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text(text = "Download Location", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Downloads are saved directly to:\n/storage/emulated/0/Download/VideoDownloader",
                    fontSize = 14.sp,
                    color = AppColors.textPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("OK", color = Color(0xFF2563EB))
                }
            },
            containerColor = AppColors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Reusable Settings Row Components
// ---------------------------------------------------------------------------

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Icon Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        // Trailing Content
        trailingContent()
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Icon Badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        // Toggle Switch
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, device = "spec:width=390dp,height=844dp")
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
