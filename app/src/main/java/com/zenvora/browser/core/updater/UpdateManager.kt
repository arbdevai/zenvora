package com.zenvora.browser.core.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.zenvora.browser.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object NoUpdateAvailable : UpdateState
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateState
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateState
    data class ReadyToInstall(val apkFile: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val downloadUrl: String,
    val releaseNotes: String,
    val fileSizeBytes: Long,
    val matchedAbi: String = "arm64-v8a",
    val assetName: String = ""
)

class UpdateManager(private val context: Context) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val updatesDir: File
        get() = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }

    /**
     * Mendeteksi arsitektur CPU utama perangkat (fokus pada arm64-v8a dan armeabi-v7a).
     */
    fun getDevicePrimaryArmAbi(): String {
        val supported = Build.SUPPORTED_ABIS ?: emptyArray()
        for (abi in supported) {
            if (abi.equals("arm64-v8a", ignoreCase = true)) return "arm64-v8a"
            if (abi.equals("armeabi-v7a", ignoreCase = true)) return "armeabi-v7a"
        }
        return supported.firstOrNull() ?: "arm64-v8a"
    }

    /**
     * Memilih aset APK dari daftar GitHub Release yang paling cocok dengan ABI perangkat:
     * 1. Prioritas utama: Nama aset mengandung ABI eksak perangkat (arm64-v8a atau armeabi-v7a).
     * 2. Prioritas kedua: Jika perangkat 64-bit (arm64-v8a) tapi hanya ada armeabi-v7a atau universal.
     * 3. Prioritas ketiga: Aset APK universal / standar pertama yang tersedia.
     */
    private fun selectBestApkAsset(assets: JSONArray?): Triple<String, Long, String>? {
        if (assets == null || assets.length() == 0) return null

        val apkAssets = mutableListOf<Triple<String, String, Long>>() // (name, url, size)
        for (i in 0 until assets.length()) {
            val obj = assets.getJSONObject(i)
            val name = obj.optString("name", "")
            val url = obj.optString("browser_download_url", "")
            val size = obj.optLong("size", 0L)
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotEmpty()) {
                apkAssets.add(Triple(name, url, size))
            }
        }

        if (apkAssets.isEmpty()) return null

        val deviceAbi = getDevicePrimaryArmAbi()

        // 1. Cari yang cocok persis dengan ABI utama perangkat (arm64-v8a atau armeabi-v7a)
        val exactMatch = apkAssets.firstOrNull { (name, _, _) ->
            name.contains(deviceAbi, ignoreCase = true)
        }
        if (exactMatch != null) {
            return Triple(exactMatch.second, exactMatch.third, "$deviceAbi (${exactMatch.first})")
        }

        // 2. Cari APK universal
        val universalMatch = apkAssets.firstOrNull { (name, _, _) ->
            name.contains("universal", ignoreCase = true)
        }
        if (universalMatch != null) {
            return Triple(universalMatch.second, universalMatch.third, "universal (${universalMatch.first})")
        }

        // 3. Jika perangkat arm64-v8a dan hanya ada armeabi-v7a (atau sebaliknya jika cuma 1 APK tanpa label ABI)
        val fallback = apkAssets.firstOrNull { (name, _, _) ->
            if (deviceAbi == "armeabi-v7a") {
                // Perangkat 32-bit TIDAK BISA memasang APK khusus arm64-v8a
                !name.contains("arm64-v8a", ignoreCase = true)
            } else {
                true
            }
        } ?: return null

        val detectedTag = when {
            fallback.first.contains("arm64-v8a", ignoreCase = true) -> "arm64-v8a"
            fallback.first.contains("armeabi-v7a", ignoreCase = true) -> "armeabi-v7a"
            else -> deviceAbi
        }
        return Triple(fallback.second, fallback.third, "$detectedTag (${fallback.first})")
    }

    suspend fun checkForUpdates(releasesApiUrl: String) = withContext(Dispatchers.IO) {
        _updateState.value = UpdateState.Checking
        cleanOldUpdateFiles()

        try {
            val request = Request.Builder()
                .url(releasesApiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _updateState.value = UpdateState.Error("Gagal memeriksa pembaruan: HTTP ${response.code}")
                return@withContext
            }

            val responseBody = response.body?.string() ?: ""
            if (responseBody.isEmpty()) {
                _updateState.value = UpdateState.NoUpdateAvailable
                return@withContext
            }

            val json = JSONObject(responseBody)
            val tagName = json.optString("tag_name", "").removePrefix("v")
            val releaseNotes = json.optString("body", "Pembaruan performa dan keamanan sistem.")
            val assets = json.optJSONArray("assets")

            val selectedAsset = selectBestApkAsset(assets)
            if (selectedAsset == null || tagName.isEmpty()) {
                _updateState.value = UpdateState.NoUpdateAvailable
                return@withContext
            }

            val (downloadUrl, fileSizeBytes, matchedAbiLabel) = selectedAsset
            val deviceAbi = getDevicePrimaryArmAbi()

            if (isNewerVersion(tagName, BuildConfig.VERSION_NAME)) {
                val info = AppUpdateInfo(
                    versionName = tagName,
                    versionCode = BuildConfig.VERSION_CODE + 1L,
                    downloadUrl = downloadUrl,
                    releaseNotes = releaseNotes,
                    fileSizeBytes = fileSizeBytes,
                    matchedAbi = deviceAbi,
                    assetName = matchedAbiLabel
                )
                _updateState.value = UpdateState.UpdateAvailable(info)
            } else {
                _updateState.value = UpdateState.NoUpdateAvailable
            }

        } catch (e: Exception) {
            _updateState.value = UpdateState.Error("Gagal terhubung ke server rilis: ${e.message}")
        }
    }

    suspend fun downloadUpdate(info: AppUpdateInfo) = withContext(Dispatchers.IO) {
        val safeAbi = info.matchedAbi.replace("[^a-zA-Z0-9_-]".toRegex(), "")
        val targetApk = File(updatesDir, "zenvora_v${info.versionName}_${safeAbi}.apk")
        val partFile = File(updatesDir, "zenvora_v${info.versionName}_${safeAbi}.apk.part")

        if (targetApk.exists() && isApkValid(targetApk, info.fileSizeBytes)) {
            _updateState.value = UpdateState.ReadyToInstall(targetApk)
            return@withContext
        }

        try {
            if (partFile.exists()) partFile.delete()

            val request = Request.Builder().url(info.downloadUrl).build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                _updateState.value = UpdateState.Error("Unduhan gagal: HTTP ${response.code}")
                return@withContext
            }

            val body = response.body ?: throw IllegalStateException("Badan respons unduhan kosong")
            val totalBytes = if (info.fileSizeBytes > 0L) info.fileSizeBytes else body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesCopied = 0L
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        val percent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                        _updateState.value = UpdateState.Downloading(percent, bytesCopied, totalBytes)
                    }
                    output.flush()
                }
            }

            if (partFile.renameTo(targetApk) && isApkValid(targetApk, info.fileSizeBytes)) {
                _updateState.value = UpdateState.ReadyToInstall(targetApk)
            } else {
                targetApk.delete()
                partFile.delete()
                _updateState.value = UpdateState.Error("Integritas berkas APK tidak valid setelah diunduh")
            }

        } catch (e: Exception) {
            if (partFile.exists()) partFile.delete()
            _updateState.value = UpdateState.Error("Proses unduhan terputus: ${e.message}")
        }
    }

    fun triggerInstall(apkFile: File) {
        if (!isApkValid(apkFile)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return
        }

        val authority = "${context.packageName}.provider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }

    private fun isApkValid(apkFile: File, expectedBytes: Long = 0L): Boolean {
        if (!apkFile.exists() || !apkFile.isFile || apkFile.length() < 10_000_000) return false
        if (expectedBytes > 0L && Math.abs(apkFile.length() - expectedBytes) > 2048L) return false

        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_ACTIVITIES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES or PackageManager.GET_ACTIVITIES
            }
            val pkg = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
            if (pkg.packageName != context.packageName) return false
            val remoteVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }
            remoteVersionCode > BuildConfig.VERSION_CODE
        } catch (_: Exception) {
            false
        }
    }

    private fun cleanOldUpdateFiles() {
        try {
            updatesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".part") || file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Abaikan kegagalan pembersihan cache
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun dismiss() {
        _updateState.value = UpdateState.Idle
    }
}
