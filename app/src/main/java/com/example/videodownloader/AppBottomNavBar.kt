package com.example.videodownloader

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors

// ---------------------------------------------------------------------------
// App-wide shared bottom navigation bar — Home (0) and Downloads (1)
// ---------------------------------------------------------------------------

enum class AppScreen { HOME, DOWNLOADS }

private data class NavBarItem(
    val label: String,
    val icon: ImageVector,
    val screen: AppScreen
)

private val appNavItems = listOf(
    NavBarItem("Home",      Icons.Filled.Home,       AppScreen.HOME),
    NavBarItem("Downloads", Icons.Outlined.Download, AppScreen.DOWNLOADS),
)

@Composable
fun AppBottomNavBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val isDark = AppColors.isDark

    NavigationBar(
        containerColor = AppColors.navBg,
        tonalElevation = 0.dp,
        modifier       = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                ambientColor = if (isDark) Color(0x66000000) else Color(0x1F1E40AF),
                spotColor = if (isDark) Color(0x88000000) else Color(0x331E40AF)
            )
            .border(width = 0.8.dp, color = AppColors.divider)
    ) {
        appNavItems.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            NavigationBarItem(
                selected = isSelected,
                onClick  = { onTabSelected(index) },
                icon     = {
                    Icon(
                        imageVector        = item.icon,
                        contentDescription = item.label,
                        modifier           = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text       = item.label,
                        fontSize   = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AppColors.blue,
                    selectedTextColor   = AppColors.blue,
                    unselectedIconColor = AppColors.textSecondary,
                    unselectedTextColor = AppColors.textSecondary,
                    indicatorColor      = Color.Transparent
                )
            )
        }
    }
}

