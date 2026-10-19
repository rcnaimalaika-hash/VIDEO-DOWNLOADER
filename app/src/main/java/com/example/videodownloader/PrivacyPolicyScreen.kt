package com.example.videodownloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors
import com.example.videodownloader.ui.theme.LocalIsDarkTheme

// ---------------------------------------------------------------------------
// Privacy Policy Section Model
// ---------------------------------------------------------------------------

private data class PolicySection(
    val id: Int,
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val fullContent: String
)

// ---------------------------------------------------------------------------
// Privacy Policy Screen (Matching privacy and policy.png)
// ---------------------------------------------------------------------------

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var expandedSectionId by remember { mutableStateOf<Int?>(null) }

    val policySections = listOf(
        PolicySection(
            id = 1,
            icon = Icons.Outlined.AdminPanelSettings,
            title = stringResource(R.string.privacy_sec1_title),
            summary = stringResource(R.string.privacy_sec1_desc),
            fullContent = stringResource(R.string.privacy_sec1_desc)
        ),
        PolicySection(
            id = 2,
            icon = Icons.Outlined.FactCheck,
            title = stringResource(R.string.privacy_sec2_title),
            summary = stringResource(R.string.privacy_sec2_desc),
            fullContent = stringResource(R.string.privacy_sec2_desc)
        ),
        PolicySection(
            id = 3,
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.privacy_sec3_title),
            summary = stringResource(R.string.privacy_sec3_desc),
            fullContent = stringResource(R.string.privacy_sec3_desc)
        ),
        PolicySection(
            id = 4,
            icon = Icons.Outlined.Share,
            title = stringResource(R.string.privacy_sec4_title),
            summary = stringResource(R.string.privacy_sec4_desc),
            fullContent = stringResource(R.string.privacy_sec4_desc)
        ),
        PolicySection(
            id = 5,
            icon = Icons.Outlined.Person,
            title = stringResource(R.string.privacy_sec5_title),
            summary = stringResource(R.string.privacy_sec5_desc),
            fullContent = stringResource(R.string.privacy_sec5_desc)
        ),
        PolicySection(
            id = 6,
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.privacy_sec6_title),
            summary = stringResource(R.string.privacy_sec6_desc),
            fullContent = stringResource(R.string.privacy_sec6_desc)
        )
    )

    fun openSupportEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@videodownloader.com")
            putExtra(Intent.EXTRA_SUBJECT, "Privacy Inquiry - Video Downloader")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Contact: support@videodownloader.com", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = AppColors.background,
        topBar = {
            PrivacyTopBar(onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── 1. Hero Shield Illustration & Header ─────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3D Privacy Shield Graphic
                    PrivacyShieldGraphic()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.privacy_header_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.privacy_header_subtitle),
                        fontSize = 14.sp,
                        color = AppColors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── 2. Structured Policy Cards ───────────────────────────────────
            itemsIndexed(policySections) { index, section ->
                val isExpanded = expandedSectionId == section.id

                PolicyCardItem(
                    section = section,
                    isExpanded = isExpanded,
                    isDark = isDark,
                    onToggle = {
                        expandedSectionId = if (isExpanded) null else section.id
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── 3. Contact Us Card ───────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { openSupportEmail() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF16253E) else Color(0xFFEFF6FF)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF263D66) else Color(0xFFD6E6FF),
                                if (isDark) Color(0xFF263D66) else Color(0xFFD6E6FF)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.privacy_contact_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val contactAnnotated = buildAnnotatedString {
                                append("If you have any questions about this Privacy Policy, please contact us at ")
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append("support@videodownloader.com")
                                }
                            }

                            Text(
                                text = contactAnnotated,
                                fontSize = 12.sp,
                                color = AppColors.textSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── 4. Last Updated Timestamp ────────────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.privacy_last_updated),
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Privacy Top Bar
// ---------------------------------------------------------------------------

@Composable
private fun PrivacyTopBar(onBack: () -> Unit) {
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
                text = stringResource(R.string.privacy_title),
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
// 3D Privacy Shield Graphic
// ---------------------------------------------------------------------------

@Composable
private fun PrivacyShieldGraphic() {
    Box(
        modifier = Modifier
            .size(160.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background sparkles & rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sparkle 1 (top-left)
            drawCircle(
                color = Color(0x334F8BFF),
                radius = 6.dp.toPx(),
                center = Offset(w * 0.18f, h * 0.3f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Sparkle 2 (top-right)
            drawCircle(
                color = Color(0x334F8BFF),
                radius = 7.dp.toPx(),
                center = Offset(w * 0.82f, h * 0.32f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 3D Shield + Padlock + Violet Checkmark Badge
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Shield Body
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0x552563EB))
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1D4ED8))
                        )
                    )
                    .border(
                        2.5.dp,
                        Brush.verticalGradient(listOf(Color(0xFF93C5FD), Color(0xFF1D4ED8))),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // White Padlock in center of shield
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }

            // Purple / Violet Checkmark Badge on Bottom-Right
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 2.dp)
                    .shadow(6.dp, CircleShape, spotColor = Color(0x668B5CF6))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))
                        )
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Policy Card Item
// ---------------------------------------------------------------------------

@Composable
private fun PolicyCardItem(
    section: PolicySection,
    isExpanded: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "policyArrowRot"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(AppColors.cardBorder, AppColors.cardBorder))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Blue Icon Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E2E4A) else Color(0xFFEEF4FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = section.summary,
                        fontSize = 13.sp,
                        color = AppColors.textSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Trailing Chevron
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 58.dp, top = 12.dp, end = 8.dp)
                ) {
                    Divider(
                        color = AppColors.divider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Text(
                        text = section.fullContent,
                        fontSize = 13.sp,
                        color = AppColors.textSecondary,
                        lineHeight = 19.sp
                    )
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
fun PrivacyPolicyScreenPreview() {
    PrivacyPolicyScreen()
}
