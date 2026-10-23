package com.example.videodownloader

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import com.example.videodownloader.ads.AdManager
import com.example.videodownloader.ads.MediumNativeAdCard
import com.example.videodownloader.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ---------------------------------------------------------------------------
// Thumbnail colour palette (deterministic per job id)
// ---------------------------------------------------------------------------

private val thumbPalette = listOf(
    Color(0xFF1B5E20), Color(0xFF1A237E), Color(0xFF4A148C),
    Color(0xFF37474F), Color(0xFF4E342E), Color(0xFF0D47A1),
    Color(0xFF006064), Color(0xFF880E4F), Color(0xFF33691E)
)

fun thumbColor(id: String) =
    thumbPalette[id.hashCode().and(0x7FFFFFFF) % thumbPalette.size]

// ---------------------------------------------------------------------------
// Reusable Media Thumbnail with Video Icon & Frame Extraction
// ---------------------------------------------------------------------------

@Composable
fun MediaThumbnail(
    job: DownloadJob,
    modifier: Modifier = Modifier
) {
    val isAudio = job.fileType == "audio" || job.format.contains("MP3", ignoreCase = true)
    val color = thumbColor(job.id)

    // Load actual video frame bitmap if available
    val videoBitmap by produceState<Bitmap?>(initialValue = null, job.localPath) {
        if (!isAudio && !job.localPath.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                try {
                    val file = File(job.localPath)
                    if (file.exists()) {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(file.absolutePath)
                        val frame = retriever.frameAtTime
                        retriever.release()
                        frame
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (videoBitmap != null) {
            Image(
                bitmap = videoBitmap!!.asImageBitmap(),
                contentDescription = "Video Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
            )
        }

        // Top-Start Video / Audio icon badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAudio) Icons.Filled.Audiotrack else Icons.Filled.Videocam,
                    contentDescription = if (isAudio) "Audio" else "Video",
                    tint = if (isAudio) Color(0xFFFFD54F) else Color(0xFF64B5F6),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isAudio) "MP3" else "VIDEO",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Center Play Icon
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xAA000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Downloads Screen  — wired to DownloadViewModel
// ---------------------------------------------------------------------------

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    themeViewModel: ThemeViewModel? = null,
    onTabSelected: (Int) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRateClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab    by remember { mutableStateOf(0) }
    var selectedNavTab by remember { mutableStateOf(1) }

    val isDarkMode = themeViewModel?.isDarkMode?.collectAsStateWithLifecycle()?.value ?: false
    var drawerItem by remember { mutableStateOf("Downloads") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val activeJobs    by viewModel.activeJobs.collectAsState()
    val completedJobs by viewModel.completedJobs.collectAsState()
    val failedJobs    by viewModel.failedJobs.collectAsState()

    val tabs = listOf(
        stringResource(R.string.dl_tab_all, completedJobs.size),
        stringResource(R.string.dl_tab_video, completedJobs.count { it.fileType == "video" }),
        stringResource(R.string.dl_tab_audio, completedJobs.count { it.fileType == "audio" })
    )

    // Filter completed & active jobs according to the active tab (0: All, 1: Video, 2: Audio)
    val displayedCompleted: List<DownloadJob> = when (selectedTab) {
        1    -> completedJobs.filter { it.fileType == "video" }
        2    -> completedJobs.filter { it.fileType == "audio" }
        else -> completedJobs
    }
    val displayedActive: List<DownloadJob> = when (selectedTab) {
        1    -> activeJobs.filter { it.fileType == "video" }
        2    -> activeJobs.filter { it.fileType == "audio" }
        else -> activeJobs
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                selectedItem = drawerItem,
                isDarkMode = isDarkMode,
                onItemClick = { item ->
                    drawerItem = item
                    scope.launch { drawerState.close() }
                    when (item) {
                        "Home"            -> { onTabSelected(0); onHomeClick() }
                        "Downloads"       -> { /* Already on downloads */ }
                        "Language"        -> onLanguageClick()
                        "Settings"        -> onSettingsClick()
                        "Rate Us"         -> onRateClick()
                        "Share App"       -> onShareClick()
                        "Help & Support"  -> onHelpClick()
                        "Privacy Policy"  -> onPrivacyClick()
                        "About App"       -> onAboutClick()
                    }
                },
                onDarkModeToggle = { themeViewModel?.setDarkMode(it) },
                onGoPremium = { scope.launch { drawerState.close() } }
            )
        },
        scrimColor = Color(0x99000000)
    ) {
        Scaffold(
            containerColor = AppColors.background,
            bottomBar = {
                AppBottomNavBar(
                    selectedIndex = selectedNavTab,
                    onTabSelected = { selectedNavTab = it; onTabSelected(it) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // ── Top Bar & Filter Chips ────────────────────────────────
                Column(modifier = Modifier.fillMaxWidth().background(AppColors.surface)) {
                    Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                    // Top Bar with 3-bar Hamburger Icon & Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = AppColors.textPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.nav_downloads),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.textPrimary
                        )
                    }

                    // 3 Filter Bars / Tabs (All / Video / Audio)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { index, label ->
                            val isSelected = index == selectedTab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) AppColors.blue else AppColors.chipBg)
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label, fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else AppColors.chipText
                                )
                            }
                        }
                    }
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
            ) {

                // Placement 7: Download Screen Medium Native Ad
                item {
                    MediumNativeAdCard()
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Active / Downloading section ──────────────────────────
                if (displayedActive.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.dl_section_downloading, displayedActive.size),
                            actionLabel = stringResource(R.string.dl_pause_all),
                            actionIcon  = Icons.Filled.Pause,
                            onAction    = { displayedActive.forEach { viewModel.pauseDownload(it.id) } }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(displayedActive, key = { it.id }) { job ->
                        ActiveDownloadCard(
                            job      = job,
                            onPause  = { viewModel.pauseDownload(job.id) },
                            onCancel = { viewModel.cancelDownload(job.id) },
                            onRemove = { viewModel.removeDownload(job.id, deleteFile = true) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ── Failed section ────────────────────────────────────────
                if (failedJobs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(
                            title = "Failed (${failedJobs.size})",
                            actionLabel = "Clear All",
                            onAction = { viewModel.clearFailedDownloads() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(failedJobs, key = { it.id }) { job ->
                        FailedDownloadCard(
                            job      = job,
                            onRetry  = {
                                AdManager.showInterstitial(
                                    context = context,
                                    placementKey = AdManager.PLACEMENT_ON_DOWNLOAD,
                                    onDismissed = { viewModel.retryDownload(job) }
                                )
                            },
                            onRemove = { viewModel.removeDownload(job.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ── Completed section ─────────────────────────────────────
                if (displayedCompleted.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(
                            title = stringResource(R.string.dl_section_downloaded, displayedCompleted.size),
                            actionLabel = null
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(displayedCompleted, key = { it.id }) { job ->
                        CompletedDownloadCard(
                            job      = job,
                            onPlay   = {
                                AdManager.showInterstitial(
                                    context = context,
                                    placementKey = AdManager.PLACEMENT_ON_DOWNLOAD,
                                    onDismissed = { viewModel.openDownloadedMedia(context, job) }
                                )
                            },
                            onShare  = { viewModel.shareDownloadedMedia(context, job) },
                            onRemove = { viewModel.removeDownload(job.id, deleteFile = true) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ── Empty state ───────────────────────────────────────────
                if (displayedActive.isEmpty() && displayedCompleted.isEmpty() && failedJobs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⬇️", fontSize = 52.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No downloads yet",
                                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                    color = AppColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Paste a link from TikTok, Instagram, Facebook, X, etc. on the Home screen to start downloading",
                                    fontSize = 13.sp, color = AppColors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ---------------------------------------------------------------------------
// Section Header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String?,
    actionIcon: ImageVector? = null,
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.textPrimary)
        if (actionLabel != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onAction() }) {
                Text(actionLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.blue)
                if (actionIcon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                            .border(1.5.dp, AppColors.blue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(actionIcon, null, tint = AppColors.blue, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Active Download Card
// ---------------------------------------------------------------------------

@Composable
private fun ActiveDownloadCard(
    job: DownloadJob,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    val animProgress by animateFloatAsState(
        targetValue    = job.progress,
        animationSpec  = tween(300),
        label          = "progress_${job.id}"
    )
    val color = thumbColor(job.id)
    val isAudio = job.fileType == "audio" || job.format.contains("MP3", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail / Icon Box
        Box(
            modifier = Modifier.size(width = 88.dp, height = 68.dp)
                .clip(RoundedCornerShape(10.dp)).background(color),
            contentAlignment = Alignment.Center
        ) {
            // Video / Audio badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAudio) Icons.Filled.Audiotrack else Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = if (isAudio) Color(0xFFFFD54F) else Color(0xFF64B5F6),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isAudio) "MP3" else "VIDEO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            val statusEmoji = when (job.status) {
                DownloadStatus.FETCHING_INFO -> "🔍"
                DownloadStatus.PAUSED        -> "⏸"
                else                         -> if (isAudio) "🎵" else "⬇️"
            }
            Text(statusEmoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                job.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${job.format}  •  ${if (job.status == DownloadStatus.FETCHING_INFO) "Connecting..." else job.formattedDownloaded + " / " + job.formattedTotal}",
                fontSize = 11.sp, color = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            if (job.isIndeterminate) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = AppColors.blue,
                    trackColor = AppColors.surface2
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(2.dp)).background(AppColors.surface2)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp)).background(AppColors.blue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            val pct = (animProgress * 100).toInt().coerceIn(0, 100)
            val durationPart = if (job.duration.isNotBlank() && job.duration != "--:--" && job.duration != "00:00") "  •  ETA ${job.duration}" else ""
            Text(
                if (job.status == DownloadStatus.FETCHING_INFO) "Fetching stream info…" else "$pct%  •  ${job.speed}$durationPart",
                fontSize = 11.sp, color = AppColors.textSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(AppColors.surface2).clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = AppColors.textSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Failed Download Card
// ---------------------------------------------------------------------------

@Composable
private fun FailedDownloadCard(
    job: DownloadJob,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.errorBg)
            .border(1.dp, AppColors.errorBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(width = 70.dp, height = 65.dp)
                .clip(RoundedCornerShape(10.dp)).background(Color(0xFFD32F2F)),
            contentAlignment = Alignment.Center
        ) { Text("❌", fontSize = 20.sp) }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                job.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                job.errorMessage ?: "Download failed. Check link or network.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.error, maxLines = 2
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(onClick = onRetry) {
            Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = AppColors.blue, modifier = Modifier.size(22.dp))
        }

        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AppColors.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Completed Download Card with MediaThumbnail, Play, Share, and Delete
// ---------------------------------------------------------------------------

@Composable
private fun CompletedDownloadCard(
    job: DownloadJob,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.surface)
            .border(1.dp, AppColors.cardBorder, RoundedCornerShape(14.dp))
            .clickable { onPlay() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with Video Icon & Frame Extraction
        MediaThumbnail(
            job = job,
            modifier = Modifier.size(width = 90.dp, height = 70.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                job.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "${job.format}  •  ${job.formattedTotal}",
                fontSize = 11.sp, color = AppColors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(job.addedAt, fontSize = 10.sp, color = AppColors.textSecondary)
        }

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = AppColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(AppColors.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Play / Open", color = AppColors.textPrimary) },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, null, tint = AppColors.blue) },
                    onClick = {
                        menuExpanded = false
                        onPlay()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share", color = AppColors.textPrimary) },
                    leadingIcon = { Icon(Icons.Filled.Share, null, tint = AppColors.blue) },
                    onClick = {
                        menuExpanded = false
                        onShare()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuExpanded = false
                        onRemove()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun DownloadScreenPreview() { /* Preview */ }
