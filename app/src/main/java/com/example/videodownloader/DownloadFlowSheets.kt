package com.example.videodownloader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.ui.theme.AppColors

// ---------------------------------------------------------------------------
// Unified Download Flow Modal Host (ModalBottomSheet above all screen contents)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadFlowModal(
    flowStep: DownloadFlowStep,
    job: DownloadJob?,
    onDismiss: () -> Unit,
    onFormatConfirmed: (fileType: String, format: String) -> Unit,
    onCancelDownload: () -> Unit,
    onViewInDownloads: () -> Unit
) {
    if (flowStep == DownloadFlowStep.NONE) return

    val currentContext = LocalContext.current
    val savedLang = remember(flowStep) { LocaleHelper.getSavedLanguage(currentContext) }
    val localizedContext = remember(savedLang) { LocaleHelper.wrapContext(currentContext, savedLang) }
    val localizedConfig = remember(savedLang) { LocaleHelper.getLocalizedConfiguration(currentContext, savedLang) }
    val layoutDirection = if (LocaleHelper.isRtl(savedLang)) LayoutDirection.Rtl else LayoutDirection.Ltr

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (flowStep == DownloadFlowStep.DOWNLOADING) {
                onCancelDownload()
            } else {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfig,
            LocalLayoutDirection provides layoutDirection
        ) {
            when (flowStep) {
                DownloadFlowStep.FORMAT_SELECTION -> {
                    DownloadFormatSelectionSheet(
                        onVideoSelect = { onFormatConfirmed("video", "best") },
                        onAudioSelect = { onFormatConfirmed("audio", "mp3") },
                        onCancel = onDismiss
                    )
                }
                DownloadFlowStep.DOWNLOADING -> {
                    DownloadProgressSheet(
                        job = job,
                        onClose = onDismiss,
                        onCancelDownload = onCancelDownload
                    )
                }
                DownloadFlowStep.COMPLETED -> {
                    DownloadCompleteSheet(
                        job = job,
                        onViewInDownloads = onViewInDownloads,
                        onClose = onDismiss
                    )
                }
                DownloadFlowStep.NONE -> {}
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Format Selection Bottom Sheet (Choose Download.png)
// ---------------------------------------------------------------------------

@Composable
fun DownloadFormatSelectionSheet(
    onVideoSelect: () -> Unit,
    onAudioSelect: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = AppColors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF94A3B8))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sheet Title & Subtitle
            Text(
                text = stringResource(R.string.flow_choose_type_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.flow_choose_type_subtitle),
                fontSize = 14.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Option 1: Video (Purple Icon, MP4 badge)
            FormatOptionCard(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3E8FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Video",
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = stringResource(R.string.flow_option_video_title),
                subtitle = stringResource(R.string.flow_option_video_subtitle),
                onClick = onVideoSelect
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Audio (Green Icon, MP3 badge)
            FormatOptionCard(
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = "Audio",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = stringResource(R.string.flow_option_audio_title),
                subtitle = stringResource(R.string.flow_option_audio_subtitle),
                onClick = onAudioSelect
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cancel Button
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.surface2,
                    contentColor = AppColors.textSecondary
                )
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Download Progress Bottom Sheet (Download Progress.png)
// ---------------------------------------------------------------------------

@Composable
fun DownloadProgressSheet(
    job: DownloadJob?,
    onClose: () -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAudio = job?.fileType == "audio"
    val progress = job?.progress ?: 0.05f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200, easing = LinearOutSlowInEasing),
        label = "sheetProgress"
    )

    val displayFileName = job?.title?.takeIf { it.isNotBlank() } ?: "video.mp4"
    val displaySize = if (job != null && job.downloadedBytes > 0 && job.totalBytes > 0) {
        "${job.formattedDownloaded} / ${job.formattedTotal}"
    } else if (job != null && job.totalBytes > 0) {
        job.formattedTotal
    } else {
        "18.5 MB"
    }

    val displayTimeRemaining = if (job != null && job.duration.isNotBlank() && job.duration != "--:--" && job.duration != "00:00") {
        job.duration
    } else if (animatedProgress in 0.01f..0.99f) {
        val estSecs = ((1f - animatedProgress) * 12).toInt().coerceAtLeast(1)
        String.format(java.util.Locale.getDefault(), "%02d:%02d", estSecs / 60, estSecs % 60)
    } else {
        "00:01"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = AppColors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF94A3B8))
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Header Row: Media Icon, Title/Subtitle, Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Media Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isAudio) Color(0xFFDCFCE7) else Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAudio) Icons.Filled.Audiotrack else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (isAudio) Color(0xFF16A34A) else Color(0xFF9333EA),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (isAudio) R.string.flow_downloading_audio else R.string.flow_downloading_video
                        ),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            if (isAudio) R.string.flow_just_audio else R.string.flow_just_video
                        ),
                        fontSize = 13.sp,
                        color = AppColors.textSecondary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = AppColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Linear Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppColors.blue)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Percentage Text
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondary,
                modifier = Modifier.align(Alignment.End)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Download Info Card
            DownloadInfoCard(
                fileName = displayFileName,
                size = displaySize,
                timeRemaining = displayTimeRemaining
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cancel Download Button (Soft pink/red container)
            Button(
                onClick = onCancelDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFE4E6),
                    contentColor = Color(0xFFEF4444)
                ),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Text(
                    text = stringResource(R.string.flow_cancel_download),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Download Complete Bottom Sheet (Download Complete.png)
// ---------------------------------------------------------------------------

@Composable
fun DownloadCompleteSheet(
    job: DownloadJob?,
    onViewInDownloads: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAudio = job?.fileType == "audio"
    val displayFileName = job?.title?.takeIf { it.isNotBlank() } ?: "Amazing Nature Video.mp4"
    val displaySize = if (job != null && job.totalBytes > 0) job.formattedTotal else "23.4 MB"
    val displayDuration = if (job != null && job.duration.isNotBlank() && job.duration != "--:--") {
        job.duration
    } else {
        "00:18"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = AppColors.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF94A3B8))
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Success Checkmark Badge with Confetti Dots
            Box(
                modifier = Modifier
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Floating confetti accents
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 2.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 8.dp, y = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFA855F7))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )

                // Main circular green badge
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
                text = stringResource(R.string.flow_complete_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = stringResource(
                    if (isAudio) R.string.flow_just_audio else R.string.flow_just_video
                ),
                fontSize = 14.sp,
                color = AppColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Download Info Card
            DownloadInfoCard(
                fileName = displayFileName,
                size = displaySize,
                timeRemaining = displayDuration
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Button: "View In Downloads"
            Button(
                onClick = onViewInDownloads,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.blue,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.flow_view_in_downloads),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Button: "Close"
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AppColors.divider),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.textPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.flow_close),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Shared Reusable UI Components
// ---------------------------------------------------------------------------

@Composable
private fun FormatOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
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

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AppColors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DownloadInfoCard(
    fileName: String,
    size: String,
    timeRemaining: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surface2)
            .padding(16.dp)
    ) {
        Column {
            // File Name Section
            Text(
                text = stringResource(R.string.flow_label_filename),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fileName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Size & Time Remaining Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.flow_label_size),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = size,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.flow_label_time_remaining),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeRemaining,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.textPrimary
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Standalone Screen Composables & Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, device = "spec:width=390dp,height=844dp")
@Composable
fun FormatSelectionSheetPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF135DE9)),
        contentAlignment = Alignment.BottomCenter
    ) {
        DownloadFormatSelectionSheet(
            onVideoSelect = {},
            onAudioSelect = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=390dp,height=844dp")
@Composable
fun DownloadProgressSheetPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF135DE9)),
        contentAlignment = Alignment.BottomCenter
    ) {
        DownloadProgressSheet(
            job = DownloadJob(
                title = "Amazing Nature Video.mp4",
                totalBytes = 24_536_678L,
                downloadedBytes = 15_948_840L,
                progress = 0.65f,
                duration = "00:18"
            ),
            onClose = {},
            onCancelDownload = {}
        )
    }
}

@Preview(showBackground = true, device = "spec:width=390dp,height=844dp")
@Composable
fun DownloadCompleteSheetPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF135DE9)),
        contentAlignment = Alignment.BottomCenter
    ) {
        DownloadCompleteSheet(
            job = DownloadJob(
                title = "Amazing Nature Video.mp4",
                totalBytes = 24_536_678L,
                downloadedBytes = 24_536_678L,
                progress = 1.0f,
                duration = "00:18",
                status = DownloadStatus.COMPLETED
            ),
            onViewInDownloads = {},
            onClose = {}
        )
    }
}
