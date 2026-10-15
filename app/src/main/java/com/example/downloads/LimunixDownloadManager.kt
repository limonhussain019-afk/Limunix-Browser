package com.example.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.database.entities.DownloadEntity
import com.example.database.entities.DownloadStatus
import com.example.repository.BrowserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LimunixDownloadManager(
    private val context: Context,
    private val repository: BrowserRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Limunix Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and status of active downloads"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startDownload(url: String, suggestedFileName: String? = null, mimeType: String = ""): String {
        val downloadId = UUID.randomUUID().toString()
        val safeFileName = sanitizeFileName(suggestedFileName ?: extractFileNameFromUrl(url))
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val targetFile = File(downloadDir, safeFileName)

        val entity = DownloadEntity(
            id = downloadId,
            url = url,
            fileName = safeFileName,
            filePath = targetFile.absolutePath,
            totalBytes = -1L,
            downloadedBytes = 0L,
            status = DownloadStatus.PENDING,
            mimeType = mimeType,
            createdAt = System.currentTimeMillis()
        )

        scope.launch {
            repository.insertDownload(entity)
            launchDownloadJob(downloadId, url, targetFile)
        }

        return downloadId
    }

    fun pauseDownload(id: String) {
        pausedFlags[id] = true
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            repository.updateDownloadStatus(id, DownloadStatus.PAUSED)
            updateNotification(id, "Paused", 0, 0, isIndeterminate = false, ongoing = false)
        }
    }

    fun resumeDownload(id: String) {
        pausedFlags.remove(id)
        scope.launch {
            val download = repository.getDownload(id) ?: return@launch
            val targetFile = File(download.filePath)
            launchDownloadJob(id, download.url, targetFile)
        }
    }

    fun cancelDownload(id: String) {
        pausedFlags.remove(id)
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            val download = repository.getDownload(id)
            if (download != null) {
                val file = File(download.filePath)
                if (file.exists()) file.delete()
            }
            repository.updateDownloadStatus(id, DownloadStatus.CANCELLED)
            notificationManager.cancel(id.hashCode())
        }
    }

    fun retryDownload(id: String) {
        resumeDownload(id)
    }

    fun deleteDownload(id: String, removeFile: Boolean = true) {
        cancelDownload(id)
        scope.launch {
            val download = repository.getDownload(id)
            if (removeFile && download != null) {
                val file = File(download.filePath)
                if (file.exists()) file.delete()
            }
            repository.deleteDownload(id)
        }
    }

    private fun launchDownloadJob(id: String, url: String, targetFile: File) {
        val job = scope.launch {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                repository.updateDownloadStatus(id, DownloadStatus.DOWNLOADING)

                val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

                val requestBuilder = Request.Builder().url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) LimunixBrowser/1.0")

                if (existingBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$existingBytes-")
                }

                val response = httpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    throw IllegalStateException("Server returned HTTP ${response.code}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val isRangeSupported = response.code == 206
                val totalBytes = if (isRangeSupported) {
                    existingBytes + body.contentLength()
                } else {
                    body.contentLength()
                }

                inputStream = body.byteStream()
                outputStream = FileOutputStream(targetFile, isRangeSupported)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = if (isRangeSupported) existingBytes else 0L
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeedBps = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (pausedFlags[id] == true) {
                        break
                    }
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead
                    bytesSinceLastCalc += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastSpeedCalcTime
                    if (timeDiff >= 800) {
                        currentSpeedBps = (bytesSinceLastCalc * 1000) / timeDiff
                        val remainingBytes = if (totalBytes > 0) totalBytes - downloadedBytes else 0L
                        val eta = if (currentSpeedBps > 0 && remainingBytes > 0) remainingBytes / currentSpeedBps else -1L

                        repository.updateDownloadProgress(
                            id,
                            downloadedBytes,
                            currentSpeedBps,
                            eta,
                            DownloadStatus.DOWNLOADING
                        )

                        updateNotification(
                            id = id,
                            fileName = targetFile.name,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                            isIndeterminate = totalBytes <= 0,
                            ongoing = true
                        )

                        lastSpeedCalcTime = now
                        bytesSinceLastCalc = 0L
                    }
                }

                outputStream.flush()

                if (pausedFlags[id] != true) {
                    repository.updateDownloadProgress(
                        id,
                        downloadedBytes,
                        0L,
                        0L,
                        DownloadStatus.COMPLETED
                    )
                    showCompletedNotification(id, targetFile)
                }
            } catch (e: Exception) {
                if (e is CancellationException || pausedFlags[id] == true) {
                    // Handled as paused
                } else {
                    repository.updateDownloadStatus(id, DownloadStatus.FAILED)
                    updateNotification(id, "Failed: ${targetFile.name}", 0, 0, false, ongoing = false)
                }
            } finally {
                try {
                    inputStream?.close()
                } catch (ignored: Exception) {}
                try {
                    outputStream?.close()
                } catch (ignored: Exception) {}
                activeJobs.remove(id)
            }
        }
        activeJobs[id] = job
    }

    private fun updateNotification(
        id: String,
        fileName: String,
        downloadedBytes: Long,
        totalBytes: Long,
        isIndeterminate: Boolean,
        ongoing: Boolean
    ) {
        val notificationId = id.hashCode()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)

        if (isIndeterminate) {
            builder.setProgress(0, 0, true)
            builder.setContentText("${formatBytes(downloadedBytes)} downloaded")
        } else if (totalBytes > 0) {
            val progressPercent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            builder.setProgress(100, progressPercent, false)
            builder.setContentText("$progressPercent% - ${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}")
        } else {
            builder.setContentText(fileName)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun showCompletedNotification(id: String, file: File) {
        val notificationId = id.hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText(file.name)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun openDownloadedFile(filePath: String, mimeType: String? = null) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val type = mimeType?.ifBlank { null } ?: getMimeTypeFromFile(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // Fallback generic view
        }
    }

    fun shareDownloadedFile(filePath: String, mimeType: String? = null) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val type = mimeType?.ifBlank { null } ?: getMimeTypeFromFile(file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                this.type = type
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share file").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // Handle share failure
        }
    }

    companion object {
        const val CHANNEL_ID = "limunix_downloads_channel"

        fun extractFileNameFromUrl(url: String): String {
            return try {
                val uri = Uri.parse(url)
                val lastPath = uri.lastPathSegment
                if (!lastPath.isNullOrBlank() && lastPath.contains(".")) {
                    lastPath
                } else {
                    "download_${System.currentTimeMillis()}"
                }
            } catch (e: Exception) {
                "download_${System.currentTimeMillis()}"
            }
        }

        fun sanitizeFileName(fileName: String): String {
            return fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            val df = java.text.DecimalFormat("#,##0.#")
            return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }

        fun formatSpeed(speedBps: Long): String {
            return "${formatBytes(speedBps)}/s"
        }

        fun getMimeTypeFromFile(file: File): String {
            val ext = file.extension.lowercase()
            return when (ext) {
                "mp4", "m4v" -> "video/mp4"
                "webm" -> "video/webm"
                "mkv" -> "video/x-matroska"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "ogg" -> "audio/ogg"
                "pdf" -> "application/pdf"
                "zip" -> "application/zip"
                "apk" -> "application/vnd.android.package-archive"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "html" -> "text/html"
                "txt" -> "text/plain"
                else -> "*/*"
            }
        }
    }
}
