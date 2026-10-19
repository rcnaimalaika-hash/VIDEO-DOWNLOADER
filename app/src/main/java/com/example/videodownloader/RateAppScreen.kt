package com.example.videodownloader

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
// Rate Us / Rate App Screen (with Full Dark Mode Support)
// ---------------------------------------------------------------------------

@Composable
fun RateAppScreen(
    onBack: () -> Unit = {},
    onMaybeLater: () -> Unit = onBack,
    onRateSubmitted: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    var selectedRating by remember { mutableIntStateOf(0) }

    val ratingLabels = listOf(
        stringResource(R.string.rate_lbl_0),
        stringResource(R.string.rate_lbl_1),
        stringResource(R.string.rate_lbl_2),
        stringResource(R.string.rate_lbl_3),
        stringResource(R.string.rate_lbl_4),
        stringResource(R.string.rate_lbl_5)
    )

    // Dynamic background gradient according to dark mode
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0C101A),
                Color(0xFF111724),
                Color(0xFF161E30),
                Color(0xFF0C101A)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F5FF),
                Color(0xFFF8FAFC),
                Color(0xFFFFFFFF),
                Color(0xFFEEF5FF)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        // Decorative background fluid wave at the bottom
        BottomWaveBackground(
            isDark = isDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // ── Top Bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    text = stringResource(R.string.rate_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 3D Star & Heart Hero Illustration ────────────────────────
            RateHeroIllustration(
                isDark = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Title & Subtitle ──────────────────────────────────────────
            Text(
                text = stringResource(R.string.rate_hero_enjoying),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.rate_hero_sub),
                fontSize = 14.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Tap to Rate Card ──────────────────────────────────────────
            val cardBg = if (isDark) Color(0xFF1A2234) else Color.White
            val cardBorder = if (isDark) Color(0xFF28344E) else Color(0xFFE2E8F0)
            val cardShadow = if (isDark) Color(0x66000000) else Color(0x1F1E40AF)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = cardShadow,
                        spotColor = cardShadow
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                    .padding(vertical = 22.dp, horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.rate_tap_to_rate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5 Stars Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val isSelected = i <= selectedRating
                            val scale by animateFloatAsState(
                                targetValue = if (i == selectedRating) 1.25f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                                label = "starScale$i"
                            )
                            val unselectedStarColor = if (isDark) Color(0xFF37435B) else Color(0xFF93C5FD)
                            val starColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFFFFB800) else unselectedStarColor,
                                animationSpec = tween(200),
                                label = "starColor$i"
                            )

                            IconButton(
                                onClick = { selectedRating = i },
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(scale)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$i Star",
                                    tint = starColor,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = ratingLabels[selectedRating.coerceIn(0, 5)],
                        fontSize = 13.sp,
                        fontWeight = if (selectedRating > 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedRating > 0) AppColors.blue else AppColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Primary Button: Rate Us Now ───────────────────────────────
            Button(
                onClick = {
                    onRateSubmitted(selectedRating)
                    launchPlayStore(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(54.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color(0x332563EB),
                        spotColor = Color(0x4D2563EB)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB)
                ),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.rate_submit_btn),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Secondary Action: Maybe Later ─────────────────────────────
            val laterBtnColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onMaybeLater() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = laterBtnColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.rate_maybe_later_btn),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = laterBtnColor
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Footer Thank You Text ─────────────────────────────────────
            Text(
                text = "Thank you for your support!",
                fontSize = 13.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Rate Hero Illustration (3D Blue Star Bubble + Purple Heart Bubble)
// ---------------------------------------------------------------------------

@Composable
private fun RateHeroIllustration(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Decorative Dot Matrix in top right
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = 10.dp)
                .size(width = 40.dp, height = 70.dp)
        ) {
            val dotColor = if (isDark) Color(0x3360A5FA) else Color(0x293B82F6)
            val rows = 6
            val cols = 3
            val spacing = 12f
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    drawCircle(
                        color = dotColor,
                        radius = 2.5f,
                        center = Offset(c * spacing + 10f, r * spacing + 10f)
                    )
                }
            }
        }

        // Decorative Sparkle burst rays on top right of the bubble
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 65.dp, y = (-55).dp)
                .size(40.dp)
        ) {
            val rayColor = Color(0xFF60A5FA)
            val stroke = 3.5f
            drawLine(
                color = rayColor,
                start = Offset(6f, 2f),
                end = Offset(10f, 16f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = rayColor,
                start = Offset(14f, 20f),
                end = Offset(28f, 14f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = rayColor,
                start = Offset(12f, 26f),
                end = Offset(26f, 32f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        // Floating small circle outline on top left
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 80.dp, y = (-45).dp)
                .size(16.dp)
        ) {
            drawCircle(
                color = if (isDark) Color(0x6660A5FA) else Color(0x4D3B82F6),
                radius = 6f,
                style = Stroke(width = 2.5f)
            )
        }

        // Floating small filled dot on right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-80).dp, y = 30.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x5560A5FA) else Color(0x3360A5FA))
        )

        // Soft glow shadow below main bubble
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 55.dp)
                .size(width = 110.dp, height = 24.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0x332563EB) else Color(0x1F2563EB))
        )

        // ── Main Blue Star Speech Bubble ──────────────────────────────
        Box(
            modifier = Modifier
                .size(136.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = SpeechBubbleShape(),
                    ambientColor = Color(0x332563EB),
                    spotColor = Color(0x661D4ED8)
                )
                .clip(SpeechBubbleShape())
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5B9DFF),
                            Color(0xFF3B82F6),
                            Color(0xFF2563EB)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner Specular Light reflection overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x4DFFFFFF), Color.Transparent),
                            radius = 160f,
                            center = Offset(70f, 40f)
                        )
                    )
            )

            // 3D White Star
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(68.dp)
                    .offset(y = (-3).dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = Color(0x33000000)
                    )
            )
        }

        // ── Floating Small Purple Heart Bubble on Left ────────────────
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-75).dp, y = 30.dp)
                .size(52.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = MiniHeartBubbleShape(),
                    ambientColor = Color(0x228B5CF6),
                    spotColor = Color(0x447C3AED)
                )
                .clip(MiniHeartBubbleShape())
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFA78BFA),
                            Color(0xFF8B5CF6),
                            Color(0xFF7C3AED)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Speech Bubble Shape (Rounded circle with small speech tail at bottom-left)
// ---------------------------------------------------------------------------

private fun SpeechBubbleShape() = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = w * 0.44f

    val cx = w * 0.5f
    val cy = h * 0.48f

    addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r))

    val tailPath = Path().apply {
        moveTo(w * 0.32f, h * 0.82f)
        lineTo(w * 0.18f, h * 0.98f)
        lineTo(w * 0.48f, h * 0.90f)
        close()
    }
    addPath(tailPath)
}

// ---------------------------------------------------------------------------
// Mini Heart Bubble Shape
// ---------------------------------------------------------------------------

private fun MiniHeartBubbleShape() = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = w * 0.42f
    val cx = w * 0.5f
    val cy = h * 0.48f

    addOval(androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r))

    val tailPath = Path().apply {
        moveTo(w * 0.68f, h * 0.80f)
        lineTo(w * 0.85f, h * 0.96f)
        lineTo(w * 0.55f, h * 0.88f)
        close()
    }
    addPath(tailPath)
}

// ---------------------------------------------------------------------------
// Bottom Wave Background
// ---------------------------------------------------------------------------

@Composable
private fun BottomWaveBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val wavePath = Path().apply {
            moveTo(0f, h * 0.4f)
            cubicTo(
                w * 0.3f, h * 0.1f,
                w * 0.7f, h * 0.7f,
                w, h * 0.3f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        val waveColors = if (isDark) {
            listOf(
                Color(0x1A1E293B),
                Color(0x330F172A)
            )
        } else {
            listOf(
                Color(0x33DCEBFE),
                Color(0x66BFDBFE)
            )
        }

        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(colors = waveColors)
        )
    }
}

// ---------------------------------------------------------------------------
// Play Store Launcher Helper
// ---------------------------------------------------------------------------

private fun launchPlayStore(context: Context) {
    val packageName = context.packageName
    val marketUri = Uri.parse("market://details?id=$packageName")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

    val marketIntent = Intent(Intent.ACTION_VIEW, marketUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {}
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun RateAppScreenPreview() {
    RateAppScreen()
}
