package com.example.videodownloader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "DownloadRepository"
private const val PREFS_NAME = "download_repo_prefs"
private const val KEY_JOBS_JSON = "saved_jobs_json"

// ---------------------------------------------------------------------------
// Download status
// ---------------------------------------------------------------------------

enum class DownloadStatus { QUEUED, FETCHING_INFO, DOWNLOADING, COMPLETED, FAILED, PAUSED, CANCELLED }

// ---------------------------------------------------------------------------
// Single download job
// ---------------------------------------------------------------------------

data class DownloadJob(
    val id: String             = UUID.randomUUID().toString(),
    val url: String            = "",
    val title: String          = "Downloading…",
    val thumbnailUrl: String?  = null,
    val resolution: String     = "--",
    val format: String         = "best",
    val fileType: String       = "video",   // "video" | "audio"
    val totalBytes: Long       = 0L,
    val downloadedBytes: Long  = 0L,
    val speed: String          = "0 KB/s",
    val duration: String       = "--:--",
    val localPath: String?     = null,
    val addedAt: String        = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date()),
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String?  = null,
    val progress: Float        = 0f
) {
    val isIndeterminate: Boolean    get() = status == DownloadStatus.DOWNLOADING && totalBytes <= 0
    val formattedTotal: String      get() = if (totalBytes > 0) formatBytes(totalBytes) else if (downloadedBytes > 0) formatBytes(downloadedBytes) else "..."
    val formattedDownloaded: String get() = formatBytes(downloadedBytes)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("title", title)
        put("thumbnailUrl", thumbnailUrl ?: "")
        put("resolution", resolution)
        put("format", format)
        put("fileType", fileType)
        put("totalBytes", totalBytes)
        put("downloadedBytes", downloadedBytes)
        put("speed", speed)
        put("duration", duration)
        put("localPath", localPath ?: "")
        put("addedAt", addedAt)
        put("status", status.name)
        put("errorMessage", errorMessage ?: "")
        put("progress", progress.toDouble())
    }

    companion object {
        fun formatBytes(b: Long): String = when {
            b >= 1_073_741_824L -> "%.1f GB".format(b / 1_073_741_824.0)
            b >= 1_048_576L     -> "%.1f MB".format(b / 1_048_576.0)
            b >= 1_024L         -> "%.1f KB".format(b / 1_024.0)
            else                -> "$b B"
        }

        fun fromJson(json: JSONObject): DownloadJob? {
            return try {
                val statusStr = json.optString("status", DownloadStatus.COMPLETED.name)
                val status = try { DownloadStatus.valueOf(statusStr) } catch (_: Exception) { DownloadStatus.COMPLETED }
                DownloadJob(
                    id              = json.optString("id", UUID.randomUUID().toString()),
                    url             = json.optString("url", ""),
                    title           = json.optString("title", "Downloaded Media"),
                    thumbnailUrl    = json.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    resolution      = json.optString("resolution", "--"),
                    format          = json.optString("format", "MP4"),
                    fileType        = json.optString("fileType", "video"),
                    totalBytes      = json.optLong("totalBytes", 0L),
                    downloadedBytes = json.optLong("downloadedBytes", 0L),
                    speed           = json.optString("speed", "0 KB/s"),
                    duration        = json.optString("duration", "--:--"),
                    localPath       = json.optString("localPath").takeIf { it.isNotBlank() },
                    addedAt         = json.optString("addedAt", ""),
                    status          = status,
                    errorMessage    = json.optString("errorMessage").takeIf { it.isNotBlank() },
                    progress        = json.optDouble("progress", 1.0).toFloat()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Repository — Downloads stream via /process1 with fallback to /process2
// ---------------------------------------------------------------------------

class DownloadRepository(private val context: Context) {

    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val activeCoroutines = ConcurrentHashMap<String, Job>()

    init {
        loadPersistedJobs()
    }

    // ── Persistence ───────────────────────────────────────────────────────

    private fun loadPersistedJobs() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(KEY_JOBS_JSON, null)
            val list = mutableListOf<DownloadJob>()

            if (!jsonStr.isNullOrBlank()) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    DownloadJob.fromJson(obj)?.let { job ->
                        // If job was previously downloading/queued when app was closed, mark as paused/failed
                        val validatedJob = if (job.status == DownloadStatus.DOWNLOADING || job.status == DownloadStatus.QUEUED || job.status == DownloadStatus.FETCHING_INFO) {
                            job.copy(status = DownloadStatus.PAUSED)
                        } else {
                            job
                        }
                        list.add(validatedJob)
                    }
                }
            }

            // Also scan files in download directories to make sure any files already downloaded are included
            scanDiskForMedia(list)

            _jobs.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved jobs", e)
        }
    }

    private fun scanDiskForMedia(existingList: MutableList<DownloadJob>) {
        try {
            val videoDir = getDownloadsDirectory("video")
            val audioDir = getDownloadsDirectory("audio")
            val knownPaths = existingList.mapNotNull { it.localPath }.toSet()

            fun scanDir(dir: File, fileType: String) {
                if (!dir.exists()) return
                val files = dir.listFiles { f -> f.isFile && !f.name.endsWith(".tmp") } ?: return
                for (file in files) {
                    if (file.absolutePath !in knownPaths && file.length() > 0) {
                        val ext = file.extension.uppercase(Locale.getDefault())
                        val job = DownloadJob(
                            id = UUID.randomUUID().toString(),
                            title = file.nameWithoutExtension,
                            localPath = file.absolutePath,
                            totalBytes = file.length(),
                            downloadedBytes = file.length(),
                            fileType = fileType,
                            format = if (fileType == "audio") "MP3" else if (ext.isNotBlank()) ext else "MP4",
                            progress = 1.0f,
                            status = DownloadStatus.COMPLETED,
                            addedAt = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                        )
                        existingList.add(job)
                    }
                }
            }

            scanDir(videoDir, "video")
            scanDir(audioDir, "audio")
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning disk for downloaded media", e)
        }
    }

    private fun savePersistedJobs() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val array = JSONArray()
            _jobs.value.forEach { job ->
                array.put(job.toJson())
            }
            prefs.edit().putString(KEY_JOBS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving jobs to storage", e)
        }
    }

    // ── Public entry point: Start or Restart a download ───────────────────
    suspend fun startDownload(
        rawUrl: String,
        format: String   = "best",
        fileType: String = "video",
        existingJobId: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {

        val id = existingJobId ?: UUID.randomUUID().toString()

        // Extract a friendly title from the URL
        val guessedTitle = rawUrl
            .substringAfterLast("/")
            .substringBefore("?")
            .takeIf { it.isNotBlank() && it.length > 2 }
            ?: "${if (fileType == "audio") "audio" else "video"}_${System.currentTimeMillis() % 10000}"

        val displayFormat = if (fileType == "audio") "MP3" else if (format == "best") "HD MP4" else "MP4"

        upsert(
            DownloadJob(
                id       = id,
                url      = rawUrl,
                title    = guessedTitle,
                fileType = fileType,
                format   = displayFormat,
                status   = DownloadStatus.FETCHING_INFO,
                errorMessage = null,
                duration = "--:--",
                progress = 0f
            )
        )

        try {
            // First attempt with /process1, fallback to /process2 if needed
            val downloadResult = executeStreamingDownload(
                endpointUrl = DownloadApi.PROCESS1_URL,
                id          = id,
                rawUrl      = rawUrl,
                format      = format,
                fileType    = fileType,
                guessedName = guessedTitle
            )

            if (downloadResult.isFailure) {
                Log.w(TAG, "/process1 failed, trying fallback /process2...")
                val fallbackResult = executeStreamingDownload(
                    endpointUrl = DownloadApi.PROCESS2_URL,
                    id          = id,
                    rawUrl      = rawUrl,
                    format      = format,
                    fileType    = fileType,
                    guessedName = guessedTitle
                )
                return@withContext fallbackResult
            }

            return@withContext downloadResult

        } catch (e: Exception) {
            Log.e(TAG, "Download error for job $id", e)
            val current = getJob(id)
            if (current.status != DownloadStatus.CANCELLED) {
                upsert(current.copy(
                    status       = DownloadStatus.FAILED,
                    errorMessage = e.message ?: "Download failed"
                ))
            }
            Result.failure(e)
        } finally {
            activeCalls.remove(id)
            activeCoroutines.remove(id)
        }
    }

    private suspend fun executeStreamingDownload(
        endpointUrl: String,
        id: String,
        rawUrl: String,
        format: String,
        fileType: String,
        guessedName: String
    ): Result<Unit> {
        val jsonPayload = JSONObject().apply {
            put("url", rawUrl)
            put("format", format)
            put("file_type", fileType)
        }.toString()

        val reqBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpointUrl)
            .post(reqBody)
            .header("Content-Type", "application/json")
            .header("Accept", "*/*")
            .build()

        val call = DownloadApi.client.newCall(request)
        activeCalls[id] = call

        var response: Response? = null
        try {
            response = call.execute()
        } catch (e: Exception) {
            if (call.isCanceled()) {
                return Result.failure(Exception("Download cancelled"))
            }
            return Result.failure(e)
        }

        if (!response.isSuccessful) {
            val errBody = try { response.body?.string() ?: "" } catch (_: Exception) { "" }
            val parsedError = parseServerError(response.code, errBody)
            return Result.failure(Exception(parsedError))
        }

        val body = response.body ?: return Result.failure(Exception("Empty response received from server"))

        // Extract filename from Content-Disposition header if present
        val contentDisposition = response.header("Content-Disposition") ?: ""
        val serverFileName = extractFilenameFromHeader(contentDisposition)

        val contentType = response.header("Content-Type") ?: ""
        val ext = when {
            serverFileName?.contains(".") == true -> serverFileName.substringAfterLast(".")
            contentType.contains("mp4")           -> "mp4"
            contentType.contains("webm")          -> "webm"
            contentType.contains("mpeg") || contentType.contains("mp3") -> "mp3"
            contentType.contains("audio")         -> "mp3"
            fileType == "audio"                   -> "mp3"
            else                                  -> "mp4"
        }

        val finalName = serverFileName
            ?: (sanitize(guessedName) + "_${System.currentTimeMillis() % 10000}.$ext")

        val targetDir = getDownloadsDirectory(fileType)
        val finalFile = File(targetDir, finalName)
        val tempFile = File(targetDir, "${finalName}.tmp")

        val headerLength = body.contentLength().takeIf { it > 0 } ?: 0L
        val estimatedTotalBytes = if (headerLength > 0) headerLength else if (fileType == "audio") 4_500_000L else 18_500_000L
        var totalBytes = headerLength
        var downloaded = 0L
        var lastUpdate = 0L
        val startTime  = System.currentTimeMillis()
        val buffer     = ByteArray(32 * 1024)

        upsert(getJob(id).copy(
            title           = finalFile.nameWithoutExtension,
            totalBytes      = if (totalBytes > 0) totalBytes else estimatedTotalBytes,
            downloadedBytes = 0L,
            duration        = "00:15",
            progress        = 0.02f,
            status          = DownloadStatus.DOWNLOADING
        ))

        try {
            tempFile.outputStream().buffered().use { out ->
                body.byteStream().use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (call.isCanceled()) {
                            tempFile.delete()
                            return Result.failure(Exception("Download cancelled"))
                        }

                        out.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 80) {
                            lastUpdate = now
                            val elapsed  = (now - startTime) / 1000.0
                            val speedBps = if (elapsed > 0) (downloaded / elapsed).toLong() else 0L

                            val effectiveTotal = if (totalBytes > 0) totalBytes else maxOf(estimatedTotalBytes, downloaded + 500_000L)
                            val rawProg = (downloaded.toFloat() / effectiveTotal).coerceIn(0.05f, 0.95f)

                            // Accurate time remaining countdown: never show 00:00 while downloading
                            val remainingSecs = if (speedBps > 1024 && effectiveTotal > downloaded) {
                                ((effectiveTotal - downloaded) / speedBps).toInt().coerceIn(1, 3600)
                            } else {
                                val remainingFraction = 1f - rawProg
                                (remainingFraction * 12).toInt().coerceAtLeast(1)
                            }

                            val formattedDuration = String.format(Locale.getDefault(), "%02d:%02d", remainingSecs / 60, remainingSecs % 60)

                            upsert(getJob(id).copy(
                                downloadedBytes = downloaded,
                                totalBytes      = effectiveTotal,
                                speed           = formatSpeed(speedBps),
                                duration        = formattedDuration,
                                progress        = rawProg,
                                status          = DownloadStatus.DOWNLOADING
                            ))
                        }
                    }
                }
            }

            if (tempFile.exists()) {
                if (finalFile.exists()) finalFile.delete()
                tempFile.renameTo(finalFile)
            }

            // Notify media scanner
            try {
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(finalFile)))
            } catch (e: Exception) {
                Log.w(TAG, "Media scanner broadcast failed", e)
            }

            val finalDisplayFormat = if (fileType == "audio") "MP3" else ext.uppercase()
            val totalElapsedSecs = ((System.currentTimeMillis() - startTime) / 1000).toInt().coerceAtLeast(1)
            val finalDurationStr = String.format(Locale.getDefault(), "%02d:%02d", totalElapsedSecs / 60, totalElapsedSecs % 60)

            // Smoothly display 100% progress before completing
            upsert(getJob(id).copy(
                title           = finalFile.nameWithoutExtension,
                localPath       = finalFile.absolutePath,
                downloadedBytes = downloaded,
                totalBytes      = downloaded,
                format          = finalDisplayFormat,
                duration        = "00:01",
                progress        = 1.0f,
                status          = DownloadStatus.DOWNLOADING
            ))

            kotlinx.coroutines.delay(450)

            upsert(getJob(id).copy(
                title           = finalFile.nameWithoutExtension,
                localPath       = finalFile.absolutePath,
                downloadedBytes = downloaded,
                totalBytes      = downloaded,
                format          = finalDisplayFormat,
                duration        = finalDurationStr,
                progress        = 1f,
                status          = DownloadStatus.COMPLETED
            ))

            return Result.success(Unit)

        } catch (e: Exception) {
            tempFile.delete()
            if (call.isCanceled()) {
                return Result.failure(Exception("Download cancelled"))
            }
            throw e
        }
    }

    // ── Helper methods ────────────────────────────────────────────────────

    private fun extractFilenameFromHeader(header: String): String? {
        if (header.isBlank()) return null
        return try {
            val filenameMatch = Regex("""filename\*?=['"]?(?:UTF-8'')?([^'";\r\n]+)['"]?""", RegexOption.IGNORE_CASE)
                .find(header)
            filenameMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseServerError(code: Int, errorBody: String): String {
        if (errorBody.isBlank()) return "Server returned error ($code)"
        return try {
            val json = JSONObject(errorBody)
            if (json.has("detail")) {
                val detail = json.get("detail")
                if (detail is String) {
                    return detail
                } else if (detail is JSONArray && detail.length() > 0) {
                    val firstErr = detail.getJSONObject(0)
                    return firstErr.optString("msg", "Validation error")
                }
            }
            if (json.has("message")) return json.getString("message")
            if (json.has("error")) return json.getString("error")
            "Server error ($code)"
        } catch (_: Exception) {
            "Server error ($code)"
        }
    }

    private fun getDownloadsDirectory(fileType: String): File {
        val primaryDir = if (fileType == "audio") {
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: File(context.filesDir, "Music")
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "Movies")
        }

        if (!primaryDir.exists()) {
            primaryDir.mkdirs()
        }
        return primaryDir
    }

    fun pauseJob(id: String) {
        val job = _jobs.value.find { it.id == id } ?: return
        activeCalls[id]?.cancel()
        activeCalls.remove(id)
        upsert(job.copy(status = DownloadStatus.PAUSED))
    }

    fun cancelJob(id: String) {
        activeCalls[id]?.cancel()
        activeCalls.remove(id)
        val job = _jobs.value.find { it.id == id }
        if (job != null) {
            upsert(job.copy(status = DownloadStatus.CANCELLED))
        }
    }

    fun removeJob(id: String, deleteLocalFile: Boolean = true) {
        activeCalls[id]?.cancel()
        activeCalls.remove(id)
        val job = _jobs.value.find { it.id == id }
        if (job != null && deleteLocalFile && job.localPath != null) {
            try {
                val f = File(job.localPath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete local file: ${job.localPath}", e)
            }
        }
        _jobs.value = _jobs.value.filter { it.id != id }
        savePersistedJobs()
    }

    fun clearFailedJobs() {
        _jobs.value = _jobs.value.filter { it.status != DownloadStatus.FAILED && it.status != DownloadStatus.CANCELLED }
        savePersistedJobs()
    }

    private fun getJob(id: String): DownloadJob =
        _jobs.value.find { it.id == id } ?: DownloadJob(id = id)

    private fun upsert(job: DownloadJob) {
        val list  = _jobs.value.toMutableList()
        val index = list.indexOfFirst { it.id == job.id }
        if (index >= 0) list[index] = job else list.add(0, job)
        _jobs.value = list
        savePersistedJobs()
    }

    private fun formatSpeed(bps: Long) = when {
        bps >= 1_048_576L -> "%.1f MB/s".format(bps / 1_048_576.0)
        bps >= 1_024L     -> "%.1f KB/s".format(bps / 1_024.0)
        else              -> "$bps B/s"
    }

    private fun sanitize(name: String) =
        name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "_").take(50).trim()
}
