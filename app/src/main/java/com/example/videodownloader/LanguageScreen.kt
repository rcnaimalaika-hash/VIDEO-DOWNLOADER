package com.example.videodownloader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

data class Language(
    val code: String,        // short label shown in the circle
    val name: String,        // English name
    val nativeName: String,  // native script name
    val flag: String         // country flag emoji
)

private val languages = listOf(
    Language("EN",      "English",   "English",   "🇬🇧"),
    Language("اردو",    "Urdu",      "اردو",      "🇵🇰"),
    Language("हि",      "Hindi",     "हिंदी",     "🇮🇳"),
    Language("العربية", "Arabic",    "العربية",   "🇸🇦"),
    Language("বাংলা",  "Bengali",   "বাংলা",     "🇧🇩"),
    Language("ਪੰਜਾਬੀ", "Punjabi",   "ਪੰਜਾਬੀ",   "🇵🇰"),
    Language("தமிழ்",  "Tamil",     "தமிழ்",     "🇱🇰"),
    Language("తెలుగు", "Telugu",    "తెలుగు",    "🇮🇳"),
    Language("മലയാളം", "Malayalam", "മലയാളം",    "🇮🇳"),
    Language("日本語",  "Japanese",  "日本語",     "🇯🇵"),
)

// ---------------------------------------------------------------------------
// Language Screen
// ---------------------------------------------------------------------------

@Composable
fun LanguageScreen(onContinue: (Language) -> Unit = {}) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val currentSaved = remember { LocaleHelper.getSavedLanguage(context) }
    var selected by remember {
        mutableStateOf(languages.find { it.name.equals(currentSaved, ignoreCase = true) } ?: languages.first())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 36.dp))

        // ── Title ─────────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.language_title),
            fontSize = if (isLandscape) 18.sp else 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Subtitle ──────────────────────────────────────────────────────
        Text(
            text = stringResource(R.string.language_subtitle),
            fontSize = if (isLandscape) 12.sp else 14.sp,
            fontWeight = FontWeight.Normal,
            color = AppColors.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = if (isLandscape) 16.sp else 20.sp
        )

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 20.dp))

        // ── Language list ─────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(languages) { lang ->
                LanguageRow(
                    language = lang,
                    isSelected = lang == selected,
                    onClick = { selected = lang }
                )
            }
        }

        // ── Continue button ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .border(width = 0.8.dp, color = AppColors.divider)
                .padding(horizontal = 16.dp, vertical = if (isLandscape) 8.dp else 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isLandscape) 46.dp else 54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppColors.blue)
                    .clickable { onContinue(selected) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.btn_continue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Single language row
// ---------------------------------------------------------------------------

@Composable
private fun LanguageRow(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = AppColors.isDark
    val borderColor = if (isSelected) AppColors.blue else AppColors.cardBorder
    val bgColor     = if (isSelected) AppColors.selectedBg else AppColors.surface
    val flagCircleBg = if (isSelected) {
        if (isDark) Color(0xFF1E3A6E) else Color(0xFFDEEAFF)
    } else {
        AppColors.surface2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ── Flag emoji circle ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(flagCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = language.flag,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // ── Language names ────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary
            )
            Text(
                text = language.nativeName,
                fontSize = 12.sp,
                color = AppColors.textSecondary
            )
        }

        // ── Checkmark / empty radio ───────────────────────────────────────
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AppColors.blue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, if (isDark) Color(0xFF4B5569) else AppColors.divider, CircleShape)
                    .background(AppColors.surface)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun LanguageScreenPreview() {
    LanguageScreen()
}

