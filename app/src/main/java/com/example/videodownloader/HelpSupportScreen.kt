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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors
import com.example.videodownloader.ui.theme.LocalIsDarkTheme

// ---------------------------------------------------------------------------
// Help & Support Data Models
// ---------------------------------------------------------------------------

private data class FaqItem(
    val id: Int,
    val icon: ImageVector,
    val question: String,
    val answer: String
)

private data class ContactChannel(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val lightBg: Color,
    val darkBg: Color,
    val iconColor: Color,
    val onClick: () -> Unit
)

// ---------------------------------------------------------------------------
// Help & Support Screen (Matching Help & Support.png)
// ---------------------------------------------------------------------------

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var expandedFaqId by remember { mutableStateOf<Int?>(null) }

    val allFaqs = listOf(
        FaqItem(
            id = 1,
            icon = Icons.Outlined.FileDownload,
            question = stringResource(R.string.faq_q1),
            answer = stringResource(R.string.faq_a1)
        ),
        FaqItem(
            id = 2,
            icon = Icons.Outlined.Folder,
            question = stringResource(R.string.faq_q2),
            answer = stringResource(R.string.faq_a2)
        ),
        FaqItem(
            id = 3,
            icon = Icons.Outlined.HighQuality,
            question = stringResource(R.string.faq_q3),
            answer = stringResource(R.string.faq_a3)
        ),
        FaqItem(
            id = 4,
            icon = Icons.Outlined.PauseCircleOutline,
            question = stringResource(R.string.faq_q4),
            answer = stringResource(R.string.faq_a4)
        ),
        FaqItem(
            id = 5,
            icon = Icons.Outlined.Shield,
            question = stringResource(R.string.faq_q5),
            answer = stringResource(R.string.faq_a5)
        )
    )

    val filteredFaqs = remember(searchQuery) {
        if (searchQuery.isBlank()) allFaqs
        else allFaqs.filter {
            it.question.contains(searchQuery, ignoreCase = true) ||
            it.answer.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = AppColors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HelpTopBar(onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── 1. Hero Support Illustration & Header ────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // 3D Support Headset Graphic
                    SupportHeadsetGraphic()

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.help_header_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.help_header_subtitle),
                        fontSize = 14.sp,
                        color = AppColors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Search input
                    HelpSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── 2. Frequently Asked Questions Section ────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.help_faq_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )

                    Text(
                        text = stringResource(R.string.help_faq_view_all) + " ›",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable {
                            searchQuery = ""
                            expandedFaqId = if (expandedFaqId == null) 1 else null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // FAQ Items Container
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(AppColors.cardBorder, AppColors.cardBorder)))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        filteredFaqs.forEachIndexed { index, faq ->
                            val isExpanded = expandedFaqId == faq.id
                            FaqRowItem(
                                faq = faq,
                                isExpanded = isExpanded,
                                isDark = isDark,
                                onToggle = {
                                    expandedFaqId = if (isExpanded) null else faq.id
                                }
                            )

                            if (index < filteredFaqs.lastIndex) {
                                Divider(
                                    color = AppColors.divider,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── 3. 24/7 Footer Trust Banner ──────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E2E4A) else Color(0xFFEEF4FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.help_24_7_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.help_24_7_sub),
                            fontSize = 12.sp,
                            color = AppColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Help & Support Top Bar
// ---------------------------------------------------------------------------

@Composable
private fun HelpTopBar(onBack: () -> Unit) {
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
                text = stringResource(R.string.help_title),
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
// 3D Support Headset Graphic
// ---------------------------------------------------------------------------

@Composable
private fun SupportHeadsetGraphic() {
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
                radius = 7.dp.toPx(),
                center = Offset(w * 0.15f, h * 0.35f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Sparkle 2 (top-right)
            drawCircle(
                color = Color(0x334F8BFF),
                radius = 8.dp.toPx(),
                center = Offset(w * 0.85f, h * 0.38f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 3D Headphone Headband + Chat Bubble + Question Badge
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Headphone Headband Ring
            Canvas(modifier = Modifier.size(110.dp)) {
                val strokeWidth = 10.dp.toPx()
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF2563EB), Color(0xFF4F8BFF), Color(0xFF1D4ED8), Color(0xFF2563EB))
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }

            // Left Ear Cushion
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 36.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp, y = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
                    )
            )

            // Right Ear Cushion
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 36.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-4).dp, y = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)))
                    )
            )

            // Center Speech Bubble with 3 Dots
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .offset(y = 2.dp)
                    .shadow(8.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0x662563EB))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF1E40AF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            // Pink Question Mark Badge on Top-Right
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-2).dp)
                    .shadow(6.dp, CircleShape, spotColor = Color(0x66FF3366))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF5376), Color(0xFFE11D48))
                        )
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Help Search Bar
// ---------------------------------------------------------------------------

@Composable
private fun HelpSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x11000000))
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.help_search_placeholder),
                        fontSize = 15.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF2563EB),
                        focusedTextColor = AppColors.textPrimary,
                        unfocusedTextColor = AppColors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// FAQ Row Item (Accordion)
// ---------------------------------------------------------------------------

@Composable
private fun FaqRowItem(
    faq: FaqItem,
    isExpanded: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "arrowRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Blue Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E2E4A) else Color(0xFFEEF4FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = faq.icon,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Question
            Text(
                text = faq.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary,
                modifier = Modifier.weight(1f)
            )

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
                    .padding(start = 54.dp, top = 10.dp, end = 8.dp)
            ) {
                Text(
                    text = faq.answer,
                    fontSize = 13.sp,
                    color = AppColors.textSecondary,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Contact Channel Card (2x2 / 4-card row)
// ---------------------------------------------------------------------------

@Composable
private fun ContactChannelCard(
    channel: ContactChannel,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { channel.onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) channel.darkBg else channel.lightBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = channel.icon,
                contentDescription = channel.title,
                tint = channel.iconColor,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = channel.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = channel.subtitle,
                fontSize = 10.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Still Need Help Card
// ---------------------------------------------------------------------------

@Composable
private fun StillNeedHelpCard(
    onContactClick: () -> Unit,
    isDark: Boolean
) {
    val bgGradient = if (isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF17243B), Color(0xFF1B2C4B)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFF0F6FF), Color(0xFFE5EFFF)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFFD0E1FD), Color(0xFFD0E1FD)))
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Envelope Graphic
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1A3560) else Color(0xFFDCEAFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MarkEmailRead,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text & Button
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.help_still_need_help),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stringResource(R.string.help_still_need_help_sub),
                        fontSize = 12.sp,
                        color = AppColors.textSecondary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onContactClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.help_btn_contact_us),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
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
fun HelpSupportScreenPreview() {
    HelpSupportScreen()
}
