package com.zenvora.browser.core.download

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DownloadItem(
    val id: String,
    val url: String,
    val filename: String,
    val progressPercent: Int = 0,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class BrowserDownloadManager(private val context: Context) {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun startDownload(url: String, suggestedFilename: String) = withContext(Dispatchers.IO) {
        val downloadId = UUID.randomUUID().toString()
        val safeFilename = if (suggestedFilename.isNotBlank()) {
            suggestedFilename
        } else {
            url.substringAfterLast("/").substringBefore("?").ifEmpty { "download_${System.currentTimeMillis()}" }
        }

        val item = DownloadItem(
            id = downloadId,
            url = url,
            filename = safeFilename
        )
        _downloads.update { it + item }

        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir

        val targetFile = File(downloadDir, safeFilename)
        val partFile = File(downloadDir, "$safeFilename.part")

        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                updateItem(downloadId) { it.copy(errorMessage = "Gagal mengunduh: HTTP ${response.code}") }
                return@withContext
            }

            val body = response.body ?: throw IllegalStateException("Respons kosong")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesCopied = 0L
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                        updateItem(downloadId) { it.copy(progressPercent = percent) }
                    }
                    output.flush()
                }
            }

            if (partFile.renameTo(targetFile)) {
                updateItem(downloadId) { it.copy(isCompleted = true, progressPercent = 100) }
            } else {
                updateItem(downloadId) { it.copy(errorMessage = "Gagal memindahkan berkas hasil unduhan") }
            }

        } catch (e: Exception) {
            if (partFile.exists()) partFile.delete()
            updateItem(downloadId) { it.copy(errorMessage = e.message ?: "Terjadi kesalahan unduhan") }
        }
    }

    private fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        _downloads.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }
}
