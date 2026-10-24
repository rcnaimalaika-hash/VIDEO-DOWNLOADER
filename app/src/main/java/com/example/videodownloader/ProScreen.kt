package com.example.videodownloader

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.firebase.AnalyticsManager
import com.example.videodownloader.ui.theme.AppColors
import com.example.videodownloader.ui.theme.LocalIsDarkTheme

// ---------------------------------------------------------------------------
// Benefit data model
// ---------------------------------------------------------------------------

private data class BenefitItem(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val titleRes: Int,
    val subtitleRes: Int
)

// ---------------------------------------------------------------------------
// Single-Screen Pro Screen with High-Visibility Tags
// ---------------------------------------------------------------------------

@Composable
fun ProScreen(
    onBack: () -> Unit = {},
    onSelectPlan: (String) -> Unit = {},
    onGoPremium: () -> Unit = {}
) {
    var selectedPlan by remember { mutableStateOf("Yearly") }
    val isDark = LocalIsDarkTheme.current

    // Prices mirrored from the plan boxes below (USD) — used for GA4 e-commerce events
    val planPrices = remember { mapOf("Monthly" to 2.99, "Yearly" to 14.99, "Lifetime" to 29.99) }
    LaunchedEffect(Unit) { AnalyticsManager.logProScreenView(source = "drawer") }

    val benefits = listOf(
        BenefitItem(
            icon = Icons.Outlined.Block,
            iconBg = if (isDark) Color(0xFF2E1C1C) else Color(0xFFFFEBEE),
            iconTint = Color(0xFFEF4444),
            titleRes = R.string.pro_benefit_no_ads,
            subtitleRes = R.string.pro_benefit_no_ads_sub
        ),
        BenefitItem(
            icon = Icons.Outlined.RocketLaunch,
            iconBg = if (isDark) Color(0xFF1E283D) else Color(0xFFEFF6FF),
            iconTint = Color(0xFF3B82F6),
            titleRes = R.string.pro_benefit_faster,
            subtitleRes = R.string.pro_benefit_faster_sub
        ),
        BenefitItem(
            icon = Icons.Outlined.HighQuality,
            iconBg = if (isDark) Color(0xFF2E2413) else Color(0xFFFEF3C7),
            iconTint = Color(0xFFF59E0B),
            titleRes = R.string.pro_benefit_hd,
            subtitleRes = R.string.pro_benefit_hd_sub
        ),
        BenefitItem(
            icon = Icons.Outlined.AllInclusive,
            iconBg = if (isDark) Color(0xFF231B38) else Color(0xFFF3E8FF),
            iconTint = Color(0xFFA855F7),
            titleRes = R.string.pro_benefit_unlimited,
            subtitleRes = R.string.pro_benefit_unlimited_sub
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        // ── Compact Header Banner ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E40AF),
                            Color(0xFF2563EB),
                            Color(0xFF1D4ED8)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33FFFFFF))
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x33FFFFFF))
                            .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑", fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.pro_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.pro_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = stringResource(R.string.pro_subtitle),
                    fontSize = 11.sp,
                    color = Color(0xDDFFFFFF),
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Single-Screen Content Body ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-10).dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(AppColors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Section 1: Benefits List ─────────────────────────────────
            Column {
                Text(
                    text = stringResource(R.string.pro_benefits_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary
                )

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF161E2E) else Color(0xFFF8FAFC))
                        .border(1.dp, AppColors.divider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        benefits.forEachIndexed { index, benefit ->
                            CompactBenefitItemRow(
                                item = benefit,
                                title = stringResource(benefit.titleRes),
                                subtitle = stringResource(benefit.subtitleRes)
                            )
                            if (index < benefits.lastIndex) {
                                HorizontalDivider(
                                    color = AppColors.divider,
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 2: Choose Your Plan with Prominent Tags ──────────
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pro_plan_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = AppColors.blue,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.pro_secure_payments),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.blue
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 3 Prominent Plan Boxes with Floating Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Monthly Box
                    PlanBoxWithBadge(
                        title = "Monthly",
                        price = "$2.99",
                        unit = "/ month",
                        badgeText = null,
                        badgeColors = listOf(Color(0xFF64748B), Color(0xFF475569)),
                        isSelected = selectedPlan == "Monthly",
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPlan = "Monthly"
                            onSelectPlan("Monthly")
                            AnalyticsManager.logProPlanSelected("Monthly", planPrices.getValue("Monthly"))
                        }
                    )

                    // Yearly Box with SAVE 58% Tag
                    PlanBoxWithBadge(
                        title = "Yearly",
                        price = "$14.99",
                        unit = "$1.25/mo",
                        badgeText = "SAVE 58%",
                        badgeColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                        isSelected = selectedPlan == "Yearly",
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPlan = "Yearly"
                            onSelectPlan("Yearly")
                            AnalyticsManager.logProPlanSelected("Yearly", planPrices.getValue("Yearly"))
                        }
                    )

                    // Lifetime Box with BEST VALUE Tag
                    PlanBoxWithBadge(
                        title = "Lifetime",
                        price = "$29.99",
                        unit = "one-time",
                        badgeText = "BEST VALUE",
                        badgeColors = listOf(Color(0xFF9333EA), Color(0xFF7E22CE)),
                        isSelected = selectedPlan == "Lifetime",
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedPlan = "Lifetime"
                            onSelectPlan("Lifetime")
                            AnalyticsManager.logProPlanSelected("Lifetime", planPrices.getValue("Lifetime"))
                        }
                    )
                }
            }

            // ── Section 3: Bottom Action Button & Guarantee ─────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        AnalyticsManager.logProCheckoutStarted(selectedPlan, planPrices[selectedPlan] ?: 0.0)
                        onGoPremium()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = Color(0x332563EB),
                            spotColor = Color(0x662563EB)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.blue
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("👑", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Continue with $selectedPlan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "🔒 ${stringResource(R.string.pro_fine_print)}",
                    fontSize = 10.sp,
                    color = AppColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Compact Benefit Row Component
// ---------------------------------------------------------------------------

@Composable
private fun CompactBenefitItemRow(
    item: BenefitItem,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconTint,
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = AppColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF22C55E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Plan Box with Floating High-Visibility Badge
// ---------------------------------------------------------------------------

@Composable
private fun PlanBoxWithBadge(
    title: String,
    price: String,
    unit: String,
    badgeText: String?,
    badgeColors: List<Color>,
    isSelected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.blue else if (isDark) Color(0xFF2B3648) else Color(0xFFE2E8F0),
        animationSpec = tween(150),
        label = "boxBorder"
    )

    val cardBg = if (isSelected) {
        if (isDark) Color(0xFF17243B) else Color(0xFFEFF6FF)
    } else {
        if (isDark) Color(0xFF161E2E) else Color(0xFFFFFFFF)
    }

    Box(
        modifier = modifier.height(120.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Main Card Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onClick() }
                .padding(top = 16.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Plan Title
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                // Big Price
                Text(
                    text = price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AppColors.blue else AppColors.textPrimary,
                    textAlign = TextAlign.Center
                )

                // Billing Subtitle
                Text(
                    text = unit,
                    fontSize = 9.5.sp,
                    color = AppColors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Selection Radio Check Indicator
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) AppColors.blue else Color.Transparent)
                        .border(
                            width = if (isSelected) 0.dp else 1.5.dp,
                            color = if (isSelected) Color.Transparent else if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        // ── Floating High-Visibility Badge at Top Center ─────────────
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = badgeColors.first().copy(alpha = 0.4f),
                        spotColor = badgeColors.first().copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(badgeColors))
                    .padding(horizontal = 7.dp, vertical = 2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun ProScreenPreview() {
    ProScreen()
}
