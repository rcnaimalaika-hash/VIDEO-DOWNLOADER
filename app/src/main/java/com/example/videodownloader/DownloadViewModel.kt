package com.example.videodownloader

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videodownloader.firebase.AnalyticsManager
import com.example.videodownloader.firebase.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

// ---------------------------------------------------------------------------
// Download Flow Steps (Modal / Bottom Sheet state)
// ---------------------------------------------------------------------------

enum class DownloadFlowStep {
    NONE,
    FORMAT_SELECTION,
    DOWNLOADING,
    COMPLETED
}

// ---------------------------------------------------------------------------
// UI state for the download input card
// ---------------------------------------------------------------------------

data class DownloadInputState(
    val url: String           = "",
    val format: String        = "best",    // "best" | "worst"
    val fileType: String      = "video",   // "video" | "audio"
    val isLoading: Boolean    = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    val repository = DownloadRepository(application)

    // ── Supported Platforms ───────────────────────────────────────────────
    private val _platforms = MutableStateFlow<List<SupportedPlatform>>(DownloadApi.defaultPlatforms)
    val platforms: StateFlow<List<SupportedPlatform>> = _platforms.asStateFlow()

    // ── Download Flow Modal State ─────────────────────────────────────────
    private val _flowStep = MutableStateFlow(DownloadFlowStep.NONE)
    val flowStep: StateFlow<DownloadFlowStep> = _flowStep.asStateFlow()

    private val _activeFlowJobId = MutableStateFlow<String?>(null)
    val activeFlowJob: StateFlow<DownloadJob?> = combine(repository.jobs, _activeFlowJobId) { jobs, jobId ->
        if (jobId != null) jobs.find { it.id == jobId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Fetch live supported platforms in the background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val livePlatforms = DownloadApi.fetchPlatforms()
                if (livePlatforms.isNotEmpty()) {
                    _platforms.value = livePlatforms
                }
            } catch (_: Exception) {}
        }
    }

    // ── All jobs from repository ──────────────────────────────────────────
    val allJobs: StateFlow<List<DownloadJob>> = repository.jobs

    // ── Active (in-progress) downloads ───────────────────────────────────
    val activeJobs: StateFlow<List<DownloadJob>> = repository.jobs
        .map { list ->
            list.filter { it.status in listOf(
                DownloadStatus.QUEUED,
                DownloadStatus.FETCHING_INFO,
                DownloadStatus.DOWNLOADING,
                DownloadStatus.PAUSED
            )}
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Completed downloads ───────────────────────────────────────────────
    val completedJobs: StateFlow<List<DownloadJob>> = repository.jobs
        .map { list -> list.filter { it.status == DownloadStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Failed downloads ──────────────────────────────────────────────────
    val failedJobs: StateFlow<List<DownloadJob>> = repository.jobs
        .map { list -> list.filter { it.status == DownloadStatus.FAILED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Input card state ──────────────────────────────────────────────────
    private val _inputState = MutableStateFlow(DownloadInputState())
    val inputState: StateFlow<DownloadInputState> = _inputState.asStateFlow()

    // ── URL input & controls ──────────────────────────────────────────────
    fun onUrlChange(url: String) {
        _inputState.value = _inputState.value.copy(url = url, errorMessage = null)
    }

    fun onFormatChange(format: String) {
        _inputState.value = _inputState.value.copy(format = format)
    }

    fun onFileTypeChange(fileType: String) {
        _inputState.value = _inputState.value.copy(fileType = fileType)
    }

    // ── Download Flow Triggers ────────────────────────────────────────────
    fun requestDownloadFlow(customUrl: String? = null) {
        val state = _inputState.value
        val url = (customUrl ?: state.url).trim()

        if (url.isBlank()) {
            _inputState.value = state.copy(errorMessage = "Please enter a video or audio link")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _inputState.value = state.copy(errorMessage = "Please enter a valid link starting with http:// or https://")
            return
        }
        _flowStep.value = DownloadFlowStep.FORMAT_SELECTION
    }

    fun confirmFormatAndDownload(fileType: String, format: String = "best") {
        val state = _inputState.value
        val url = state.url.trim()
        _inputState.value = state.copy(fileType = fileType, format = format)

        val jobId = UUID.randomUUID().toString()
        _activeFlowJobId.value = jobId
        _flowStep.value = DownloadFlowStep.DOWNLOADING

        AnalyticsManager.logDownloadStart(url, fileType, format)
        val startedAt = SystemClock.elapsedRealtime()

        viewModelScope.launch {
            val result = repository.startDownload(
                rawUrl = url,
                format = format,
                fileType = fileType,
                existingJobId = jobId
            )
            if (result.isSuccess) {
                trackDownloadSuccess(jobId, url, fileType, format, startedAt)
                kotlinx.coroutines.delay(200)
                _inputState.value = _inputState.value.copy(url = "", successMessage = "Download completed!")
                _flowStep.value = DownloadFlowStep.COMPLETED
            } else {
                trackDownloadFailure(url, fileType, result.exceptionOrNull())
                _flowStep.value = DownloadFlowStep.NONE
                _inputState.value = _inputState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Download failed"
                )
            }
        }
    }

    fun cancelActiveFlow() {
        _activeFlowJobId.value?.let { id ->
            findJob(id)?.let { AnalyticsManager.logDownloadCancel(it.url, it.fileType) }
            repository.cancelJob(id)
        }
        _activeFlowJobId.value = null
        _flowStep.value = DownloadFlowStep.NONE
    }

    // ── Analytics helpers ─────────────────────────────────────────────────
    private fun findJob(id: String): DownloadJob? = repository.jobs.value.find { it.id == id }

    private fun trackDownloadSuccess(jobId: String, url: String, fileType: String, format: String, startedAt: Long) {
        val job = findJob(jobId)
        AnalyticsManager.logDownloadComplete(
            url           = url,
            fileType      = fileType,
            format        = format,
            durationMs    = SystemClock.elapsedRealtime() - startedAt,
            fileSizeBytes = job?.totalBytes ?: 0L
        )
    }

    private fun trackDownloadFailure(url: String, fileType: String, error: Throwable?) {
        val message = error?.message
        if (message == "Download cancelled") return   // user action, already tracked as download_cancel
        AnalyticsManager.logDownloadFailed(url, fileType, message)
        CrashReporter.log("download_failed platform=${AnalyticsManager.platformOf(url)} type=$fileType reason=$message")
    }

    fun dismissFlow() {
        _flowStep.value = DownloadFlowStep.NONE
        _activeFlowJobId.value = null
    }

    // ── Paste from clipboard (manual button) ─────────────────────────────
    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            _inputState.value = _inputState.value.copy(url = text, errorMessage = null)
        }
    }

    // ── Auto-paste on screen resume (only if URL field is empty and clipboard has a URL) ──
    fun autoPasteFromClipboard(context: Context) {
        if (_inputState.value.url.isNotBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: return
        if (text.startsWith("http://") || text.startsWith("https://")) {
            _inputState.value = _inputState.value.copy(url = text, errorMessage = null)
        }
    }

    // ── Start download (direct) ───────────────────────────────────────────
    fun startDownload(customUrl: String? = null) {
        requestDownloadFlow(customUrl)
    }

    // ── Retry failed download ─────────────────────────────────────────────
    fun retryDownload(job: DownloadJob) {
        AnalyticsManager.logDownloadRetry(job.url, job.fileType)
        val startedAt = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            val result = repository.startDownload(
                rawUrl   = job.url,
                format   = job.format,
                fileType = job.fileType,
                existingJobId = job.id
            )
            if (result.isSuccess) {
                trackDownloadSuccess(job.id, job.url, job.fileType, job.format, startedAt)
            } else {
                trackDownloadFailure(job.url, job.fileType, result.exceptionOrNull())
            }
        }
    }

    // ── Job actions ───────────────────────────────────────────────────────
    fun pauseDownload(id: String) = repository.pauseJob(id)
    fun cancelDownload(id: String) {
        findJob(id)?.let { AnalyticsManager.logDownloadCancel(it.url, it.fileType) }
        repository.cancelJob(id)
    }
    fun removeDownload(id: String, deleteFile: Boolean = true) = repository.removeJob(id, deleteFile)
    fun clearFailedDownloads() = repository.clearFailedJobs()

    // ── Media Playback & Sharing via FileProvider ─────────────────────────
    fun openDownloadedMedia(context: Context, job: DownloadJob) {
        val path = job.localPath ?: run {
            Toast.makeText(context, "File path not available", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "File no longer exists on device", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (job.fileType == "audio" || path.endsWith(".mp3", true)) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AnalyticsManager.logMediaOpened(job.fileType)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to play this file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareDownloadedMedia(context: Context, job: DownloadJob) {
        val path = job.localPath ?: return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (job.fileType == "audio" || path.endsWith(".mp3", true)) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share media via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            AnalyticsManager.logShareMedia(job.fileType)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun dismissError() {
        _inputState.value = _inputState.value.copy(errorMessage = null)
    }

    fun dismissSuccess() {
        _inputState.value = _inputState.value.copy(successMessage = null)
    }
}
