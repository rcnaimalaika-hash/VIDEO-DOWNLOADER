package com.example.videodownloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors
import com.example.videodownloader.ui.theme.LocalIsDarkTheme

// ---------------------------------------------------------------------------
// About App Screen (Matching about.png)
// ---------------------------------------------------------------------------

@Composable
fun AboutAppScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    Scaffold(
        containerColor = AppColors.background,
        topBar = {
            AboutTopBar(onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            // ── 1. Hero App Icon & Title ──────────────────────────────────────
            AboutAppLogoGraphic()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.about_app_name),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Version Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isDark) Color(0xFF1E2E4A) else Color(0xFFEFF6FF))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF263D66) else Color(0xFFDBEAFE),
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_version_badge),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.about_tagline),
                fontSize = 14.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── 2. Feature Highlights Card ───────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(AppColors.cardBorder, AppColors.cardBorder))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutFeatureRow(
                        icon = Icons.Filled.Bolt,
                        title = stringResource(R.string.about_feature_fast),
                        subtitle = stringResource(R.string.about_feature_fast_sub),
                        isDark = isDark
                    )

                    Divider(color = AppColors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    AboutFeatureRow(
                        icon = Icons.Filled.VerifiedUser,
                        title = stringResource(R.string.about_feature_safe),
                        subtitle = stringResource(R.string.about_feature_safe_sub),
                        isDark = isDark
                    )

                    Divider(color = AppColors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    AboutFeatureRow(
                        icon = Icons.Filled.HighQuality,
                        title = stringResource(R.string.about_feature_hd),
                        subtitle = stringResource(R.string.about_feature_hd_sub),
                        isDark = isDark
                    )

                    Divider(color = AppColors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    AboutFeatureRow(
                        icon = Icons.Filled.Folder,
                        title = stringResource(R.string.about_feature_manage),
                        subtitle = stringResource(R.string.about_feature_manage_sub),
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 3. App Details & Metadata Card ───────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(AppColors.cardBorder, AppColors.cardBorder))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AboutInfoRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.about_meta_version),
                        value = stringResource(R.string.about_meta_version_val),
                        isDark = isDark
                    )

                    Divider(color = AppColors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    AboutInfoRow(
                        icon = Icons.Outlined.CalendarToday,
                        title = stringResource(R.string.about_meta_release),
                        value = stringResource(R.string.about_meta_release_val),
                        isDark = isDark
                    )

                    Divider(color = AppColors.divider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    AboutInfoRow(
                        icon = Icons.Outlined.Person,
                        title = stringResource(R.string.about_meta_developer),
                        value = stringResource(R.string.about_meta_developer_val),
                        isDark = isDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. Footer ────────────────────────────────────────────────────
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.about_footer_made_with),
                fontSize = 13.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// About Top Bar
// ---------------------------------------------------------------------------

@Composable
private fun AboutTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.background)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = AppColors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = stringResource(R.string.about_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// About App Logo Graphic
// ---------------------------------------------------------------------------

@Composable
private fun AboutAppLogoGraphic() {
    Box(
        modifier = Modifier
            .size(140.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // App Icon container with blue gradient + drop shadow
        Box(
            modifier = Modifier
                .size(108.dp)
                .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0x662563EB))
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF1E40AF))
                    )
                )
                .border(
                    2.dp,
                    Brush.verticalGradient(listOf(Color(0xFF93C5FD), Color(0xFF2563EB))),
                    RoundedCornerShape(26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // White Download Arrow + Bracket Icon
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }

        // Small Red Play Button Badge on Top Right
        Box(
            modifier = Modifier
                .size(34.dp)
                .align(Alignment.TopEnd)
                .offset(x = 2.dp, y = (-2).dp)
                .shadow(6.dp, CircleShape, spotColor = Color(0x66EF4444))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFF5252), Color(0xFFDC2626))
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Feature Row Item
// ---------------------------------------------------------------------------

@Composable
private fun AboutFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1E2E4A) else Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = AppColors.textSecondary
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Info / Metadata Row Item
// ---------------------------------------------------------------------------

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    isDark: Boolean,
    isValueLink: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isValueLink, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF2563EB),
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isValueLink) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isValueLink) Color(0xFF2563EB) else AppColors.textSecondary
        )

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun AboutAppScreenPreview() {
    AboutAppScreen()
}
