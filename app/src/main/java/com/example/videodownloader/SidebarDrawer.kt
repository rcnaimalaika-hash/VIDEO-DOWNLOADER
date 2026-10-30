package com.example.videodownloader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors
import com.example.videodownloader.ui.theme.LocalIsDarkTheme

private data class SidebarNavItem(
    val icon: ImageVector,
    val labelRes: Int,
    val key: String
)

// ---------------------------------------------------------------------------
// Sidebar Drawer  — dark-mode aware
// ---------------------------------------------------------------------------

@Composable
fun SidebarDrawer(
    selectedItem: String = "Home",
    isDarkMode: Boolean = false,
    userEmail: String? = null,
    onItemClick: (String) -> Unit = {},
    onDarkModeToggle: (Boolean) -> Unit = {},
    onGoPremium: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current

    val bg          = AppColors.surface
    val dividerClr  = AppColors.divider
    val textPrimary = AppColors.textPrimary
    val iconMuted   = AppColors.textSecondary

    val primaryItems = listOf(
        SidebarNavItem(Icons.Filled.Home,          R.string.sidebar_home,      "Home"),
        SidebarNavItem(Icons.Outlined.Download,    R.string.sidebar_downloads, "Downloads"),
        SidebarNavItem(Icons.Outlined.Language,    R.string.sidebar_language,  "Language"),
    )

    val secondaryItems = listOf(
        SidebarNavItem(Icons.Filled.Star,          R.string.sidebar_rate_us,   "Rate Us"),
        SidebarNavItem(Icons.Outlined.HelpOutline, R.string.sidebar_help,      "Help & Support"),
        SidebarNavItem(Icons.Outlined.Shield,      R.string.sidebar_privacy,   "Privacy Policy"),
        SidebarNavItem(Icons.Filled.Info,          R.string.sidebar_about,     "About App"),
        SidebarNavItem(Icons.AutoMirrored.Filled.Logout, R.string.sidebar_logout, "Log Out")
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(bg)
    ) {

        // ── Blue header (always blue regardless of dark mode) ─────────────
        DrawerHeader(isDark = isDark, userEmail = userEmail)

        // ── Scrollable items ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            primaryItems.forEach { item ->
                NavMenuItem(
                    icon       = item.icon,
                    label      = stringResource(item.labelRes),
                    isSelected = selectedItem == item.key,
                    isDark     = isDark,
                    onClick    = { onItemClick(item.key) }
                )
            }

            // Dark Mode toggle — state driven by parent
            DarkModeMenuItem(
                isDarkMode   = isDarkMode,
                isDark       = isDark,
                textPrimary  = textPrimary,
                iconMuted    = iconMuted,
                onToggle     = onDarkModeToggle
            )

            Divider(
                color     = dividerClr,
                thickness = 1.dp,
                modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            secondaryItems.forEach { item ->
                NavMenuItem(
                    icon       = item.icon,
                    label      = stringResource(item.labelRes),
                    isSelected = false,
                    isDark     = isDark,
                    onClick    = { onItemClick(item.key) }
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Blue header
// ---------------------------------------------------------------------------

@Composable
private fun DrawerHeader(isDark: Boolean, userEmail: String? = null) {
    // Bottom wave color matches the drawer background
    val waveColor = if (isDark) Color(0xFF1A1D27) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1A6BFF), Color(0xFF1252CC)))
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth().height(32.dp).align(Alignment.BottomCenter)
                .background(waveColor, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp).clip(CircleShape)
                        .background(Color(0x33FFFFFF)).padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp).clip(CircleShape)
                            .background(Color(0x44FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.welcome_screen_logo),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(R.string.sidebar_app_name),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xDDFFFFFF),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nav menu item — dark-aware
// ---------------------------------------------------------------------------

@Composable
private fun NavMenuItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor   = if (isSelected) {
        if (isDark) Color(0xFF1A3A6B) else Color(0xFFEEF3FF)
    } else Color.Transparent

    val textColor = if (isSelected) AppColors.blue
                   else AppColors.textPrimary
    val iconColor = if (isSelected) AppColors.blue
                   else AppColors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

// ---------------------------------------------------------------------------
// Dark Mode toggle row — dark-aware
// ---------------------------------------------------------------------------

@Composable
private fun DarkModeMenuItem(
    isDarkMode: Boolean,
    isDark: Boolean,
    textPrimary: Color,
    iconMuted: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.DarkMode, null,
            tint = textPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.sidebar_dark_mode),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = textPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = Color(0xFF2563EB),
                uncheckedThumbColor  = Color.White,
                uncheckedTrackColor  = if (isDark) Color(0xFF3A3D52) else Color(0xFFCED3E0),
                uncheckedBorderColor = if (isDark) Color(0xFF3A3D52) else Color(0xFFCED3E0)
            )
        )
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun SidebarDrawerPreview() {
    SidebarDrawer()
}
