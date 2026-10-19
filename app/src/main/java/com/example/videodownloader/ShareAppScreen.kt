package com.example.videodownloader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Share App Screen
// ---------------------------------------------------------------------------

@Composable
fun ShareAppScreen(onBack: () -> Unit = {}) {

    val context           = LocalContext.current
    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var copied            by remember { mutableStateOf(false) }
    val referralCode      = "VDOWN2024"

    Scaffold(
        containerColor    = AppColors.background,
        snackbarHost      = { SnackbarHost(snackbarHostState) },
        bottomBar         = {
            AppBottomNavBar(
                selectedIndex = 0,
                onTabSelected = {}
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {

            // ── Top bar ───────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surface)
            ) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBackIosNew, null,
                            tint = AppColors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.share_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Hero banner card ──────────────────────────────────────────
            HeroBannerCard()

            Spacer(modifier = Modifier.height(14.dp))

            // ── Referral code card ────────────────────────────────────────
            ReferralCodeCard(
                code    = referralCode,
                copied  = copied,
                onCopy  = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("referral_code", referralCode))
                    copied = true
                    scope.launch {
                        snackbarHostState.showSnackbar("Referral code copied!")
                        delay(2000)
                        copied = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Share via card ────────────────────────────────────────────
            ShareViaCard(context = context, referralCode = referralCode)

            Spacer(modifier = Modifier.height(14.dp))

            // ── Why share card ────────────────────────────────────────────
            WhyShareCard()

            Spacer(modifier = Modifier.height(14.dp))

            // ── Invite more earn more card ────────────────────────────────
            InviteEarnCard(
                onViewRewards = {
                    scope.launch { snackbarHostState.showSnackbar("Rewards coming soon!") }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Hero Banner Card
// ---------------------------------------------------------------------------

@Composable
private fun HeroBannerCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.heroBannerBg)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.share_hero_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                lineHeight = 30.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.blue)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.share_hero_sub),
                fontSize = 13.sp,
                color = AppColors.textSecondary,
                lineHeight = 20.sp
            )
        }

        // Right illustration — box with floating icons
        Box(
            modifier = Modifier
                .size(150.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Blue box body
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF4A80F0), Color(0xFF2563EB))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("☁️⬇", fontSize = 22.sp)
            }
            // Box flaps (top)
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(18.dp)
                    .align(Alignment.Center)
                    .offset(y = (-30).dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(Color(0xFF3A6FDF))
            )

            // Floating icons
            // Heart (pink) - bottom left
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 10.dp, y = 24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5C8D)),
                contentAlignment = Alignment.Center
            ) { Text("❤️", fontSize = 16.sp) }

            // Person (blue) - top center
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 14.sp) }

            // Share (orange) - top right
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFA726)),
                contentAlignment = Alignment.Center
            ) { Text("🔗", fontSize = 14.sp) }
        }
    }
}

// ---------------------------------------------------------------------------
// Referral Code Card
// ---------------------------------------------------------------------------

@Composable
private fun ReferralCodeCard(code: String, copied: Boolean, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.share_referral_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Code box with dashed border
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, AppColors.blue, RoundedCornerShape(12.dp))
                .background(AppColors.selectedBg)
                .clickable { onCopy() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.blue,
                letterSpacing = 2.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (copied) AppColors.blue else AppColors.surface)
                    .border(1.dp, AppColors.blue, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Filled.ContentCopy, null,
                    tint = if (copied) Color.White else AppColors.blue,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (copied) "Copied!" else "Copy",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (copied) Color.White else AppColors.blue
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.share_referral_sub),
            fontSize = 12.sp,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------------------
// Share Via Card
// ---------------------------------------------------------------------------

@Composable
private fun ShareViaCard(context: Context, referralCode: String) {
    val shareText = "Download videos from any platform easily! Use my referral code $referralCode to get started. Download Video Downloader now!"

    data class SocialApp(val emoji: String, val name: String, val packageName: String?)

    val apps = listOf(
        SocialApp("📱", "WhatsApp",  "com.whatsapp"),
        SocialApp("📸", "Instagram", "com.instagram.android"),
        SocialApp("👍", "Facebook",  "com.facebook.katana"),
        SocialApp("✈️", "Telegram",  "org.telegram.messenger"),
        SocialApp("•••","More",      null)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        // Orange accent line + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFFFA726))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.share_via_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            apps.forEach { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (app.packageName != null) {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type    = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    setPackage(app.packageName)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // App not installed — fall through to generic share
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                extraShareText(context, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                ) {
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                when (app.name) {
                                    "WhatsApp"  -> Color(0xFF25D366)
                                    "Instagram" -> Color(0xFFE1306C)
                                    "Facebook"  -> Color(0xFF1877F2)
                                    "Telegram"  -> Color(0xFF0088CC)
                                    else        -> AppColors.surface2
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (app.name == "More") {
                            Icon(
                                Icons.Filled.MoreHoriz, null,
                                tint = AppColors.textSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(app.emoji, fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = app.name,
                        fontSize = 12.sp,
                        color = AppColors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun extraShareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

// ---------------------------------------------------------------------------
// Why Share Card
// ---------------------------------------------------------------------------

@Composable
private fun WhyShareCard() {
    data class Reason(val emoji: String, val bg: Color, val title: String, val subtitle: String)

    val isDark = AppColors.isDark
    val reasons = listOf(
        Reason("🎁", if (isDark) Color(0xFF1E2D4A) else Color(0xFFEEF3FF), stringResource(R.string.share_reason_1_title), stringResource(R.string.share_reason_1_sub)),
        Reason("⭐", if (isDark) Color(0xFF382F1D) else Color(0xFFFFF8E1), stringResource(R.string.share_reason_2_title), stringResource(R.string.share_reason_2_sub)),
        Reason("🛡️", if (isDark) Color(0xFF1B3322) else Color(0xFFE8F5E9), stringResource(R.string.share_reason_3_title), stringResource(R.string.share_reason_3_sub)),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.share_why_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        reasons.forEachIndexed { index, reason ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(reason.bg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(reason.emoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = reason.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary
                    )
                    Text(
                        text = reason.subtitle,
                        fontSize = 12.sp,
                        color = AppColors.textSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
            if (index < reasons.lastIndex) {
                Divider(
                    color = AppColors.divider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Invite More Earn More Card
// ---------------------------------------------------------------------------

@Composable
private fun InviteEarnCard(onViewRewards: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.heroBannerBg)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gift box illustration
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.blue),
            contentAlignment = Alignment.Center
        ) {
            Text("🎁", fontSize = 30.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.share_invite_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.share_invite_sub),
                fontSize = 12.sp,
                color = AppColors.textSecondary,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.blue)
                .clickable { onViewRewards() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.share_btn_rewards),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun ShareAppScreenPreview() {
    ShareAppScreen()
}
