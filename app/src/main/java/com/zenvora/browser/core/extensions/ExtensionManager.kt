package com.zenvora.browser.core.extensions

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class InstalledExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val isEnabled: Boolean,
    val isBuiltIn: Boolean,
    val optionsPageUrl: String? = null,
    val rawExtension: WebExtension
)

data class RecommendedAddon(
    val id: String,
    val name: String,
    val summary: String,
    val xpiDownloadUrl: String,
    val storePageUrl: String
)

class ExtensionManager(
    private val context: Context,
    private val runtime: GeckoRuntime,
    private val scope: CoroutineScope
) {

    companion object {
        const val BUILTIN_SHIELD_ID = "zenvora-shield@zenvora.browser"
        const val AMO_STORE_HOME_URL = "https://addons.mozilla.org/en-US/firefox/extensions/"

        val RECOMMENDED_ADDONS = listOf(
            RecommendedAddon(
                id = "uBlock0@raymondhill.net",
                name = "uBlock Origin",
                summary = "Pemblokir iklan, pop-under, dan anti-adblock paling ampuh dengan filter EasyList & ABPindo penuh (Setara/Lebih Kuat dari Brave).",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/en-US/firefox/addon/ublock-origin/"
            ),
            RecommendedAddon(
                id = "claude-sidebar-free",
                name = "Claude Sidebar Free",
                summary = "Asisten AI Claude terintegrasi untuk merangkum artikel, menerjemahkan halaman, dan tanya jawab cepat di Zenvora.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/claude-sidebar-free/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/en-US/firefox/addon/claude-sidebar-free/"
            ),
            RecommendedAddon(
                id = "addon@darkreader.org",
                name = "Dark Reader",
                summary = "Ubah seluruh tampilan situs web terang menjadi tema gelap OLED yang nyaman di mata.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/en-US/firefox/addon/darkreader/"
            ),
            RecommendedAddon(
                id = "sponsorBlocker@ajay.app",
                name = "SponsorBlock for YouTube",
                summary = "Lewati otomatis segmen sponsor, intro, dan promosi di dalam video YouTube.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/sponsorblock/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/en-US/firefox/addon/sponsorblock/"
            ),
            RecommendedAddon(
                id = "firefox@tampermonkey.net",
                name = "Tampermonkey",
                summary = "Pengelola userscript populer untuk menjalankan script kustom dan bypass situs modern.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/tampermonkey/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/en-US/firefox/addon/tampermonkey/"
            ),
            RecommendedAddon(
                id = "{74145f27-f039-47ce-a470-a662b129930a}",
                name = "ClearURLs",
                summary = "Hapus otomatis parameter pelacakan (utm, fbclid, igs) dari alamat URL situs.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/clearurls/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/en-US/firefox/addon/clearurls/"
            )
        )
    }

    private val controller: WebExtensionController = runtime.webExtensionController

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _installedExtensions = MutableStateFlow<List<InstalledExtensionInfo>>(emptyList())
    val installedExtensions: StateFlow<List<InstalledExtensionInfo>> = _installedExtensions.asStateFlow()

    private val _installingAddonId = MutableStateFlow<String?>(null)
    val installingAddonId: StateFlow<String?> = _installingAddonId.asStateFlow()

    private val _installProgressPercent = MutableStateFlow<Int?>(null)
    val installProgressPercent: StateFlow<Int?> = _installProgressPercent.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        setupPromptDelegate()
        refreshInstalledExtensions()
    }

    private fun setupPromptDelegate() {
        try {
            controller.promptDelegate = object : WebExtensionController.PromptDelegate {
                override fun onInstallPromptRequest(
                    extension: WebExtension,
                    permissions: Array<out String>,
                    origins: Array<out String>
                ): GeckoResult<WebExtension.PermissionPromptResponse> {
                    return GeckoResult.fromValue(
                        WebExtension.PermissionPromptResponse(
                            true,
                            true
                        )
                    )
                }

                override fun onOptionalPrompt(
                    extension: WebExtension,
                    permissions: Array<out String>,
                    origins: Array<out String>
                ): GeckoResult<AllowOrDeny> {
                    return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                }
            }
        } catch (_: Exception) {
            // Abaikan jika promptDelegate telah diatur
        }
    }

    fun refreshInstalledExtensions() {
        try {
            controller.list().accept(
                { list ->
                    if (list != null) {
                        val mapped = list.map { ext ->
                            val meta = ext.metaData
                            val isBuiltIn = ext.id == BUILTIN_SHIELD_ID || ext.location.startsWith("resource://")
                            val displayName = when {
                                ext.id == BUILTIN_SHIELD_ID -> "Zenvora Built-In Stealth Shield"
                                !meta?.name.isNullOrBlank() -> meta?.name.orEmpty()
                                else -> ext.id
                            }
                            val displayDesc = when {
                                ext.id == BUILTIN_SHIELD_ID -> "Perisai adblock bawaan Zenvora, anti-adblock defuser, AMO Store Integrator, & pembuka background play YouTube."
                                !meta?.description.isNullOrBlank() -> meta?.description.orEmpty()
                                else -> "Ekstensi WebExtension Zenvora GeckoView"
                            }
                            val versionStr = meta?.version?.ifBlank { "1.0" } ?: "1.0"
                            val enabled = meta?.enabled ?: true
                            val optionsUrl = meta?.optionsPageUrl?.takeIf { it.isNotBlank() }

                            InstalledExtensionInfo(
                                id = ext.id,
                                name = displayName,
                                version = versionStr,
                                description = displayDesc,
                                isEnabled = enabled,
                                isBuiltIn = isBuiltIn,
                                optionsPageUrl = optionsUrl,
                                rawExtension = ext
                            )
                        }.sortedByDescending { it.isBuiltIn }

                        _installedExtensions.value = mapped
                    }
                },
                { _ -> }
            )
        } catch (_: Exception) {
            // Abaikan kegagalan list ekstensi
        }
    }

    fun installFromUrl(xpiUrl: String, addonIdOrLabel: String = xpiUrl) {
        val trimmed = xpiUrl.trim()
        if (trimmed.isEmpty()) return

        _installingAddonId.value = addonIdOrLabel
        _installProgressPercent.value = 2
        _statusMessage.value = "Menyiapkan unduhan ekstensi... 2%"

        scope.launch(Dispatchers.IO) {
            var tempXpiFile: File? = null
            try {
                val addonsDir = File(context.cacheDir, "zenvora_addons").apply {
                    if (!exists()) mkdirs()
                }
                val fileTemp = File(addonsDir, "addon_${System.currentTimeMillis()}.xpi")
                tempXpiFile = fileTemp

                val request = Request.Builder()
                    .url(trimmed)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0"
                    )
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Paket .xpi kosong")
                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L

                    body.byteStream().use { input ->
                        FileOutputStream(fileTemp).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                val kbText = "${downloadedBytes / 1024} KB"
                                val pct = if (totalBytes > 0L) {
                                    ((downloadedBytes * 88L) / totalBytes).toInt().coerceIn(5, 88)
                                } else {
                                    (10 + (downloadedBytes / 16384L).toInt()).coerceIn(10, 85)
                                }
                                _installProgressPercent.value = pct
                                _statusMessage.value = "Mengunduh ekstensi... $pct% ($kbText)"
                            }
                            output.flush()
                        }
                    }
                }

                _installProgressPercent.value = 94
                _statusMessage.value = "Memasang ekstensi ke Zenvora... 94%"

                withContext(Dispatchers.Main) {
                    val localUri = fileTemp.toURI().toString()
                    controller.install(localUri).accept(
                        { installedExt ->
                            fileTemp.delete()
                            _installProgressPercent.value = 100
                            val extName = installedExt?.metaData?.name ?: addonIdOrLabel
                            _statusMessage.value = "Ekstensi $extName berhasil dipasang ke Zenvora (100%)!"
                            if (installedExt != null) {
                                controller.setAllowedInPrivateBrowsing(installedExt, true)
                            }
                            _installingAddonId.value = null
                            _installProgressPercent.value = null
                            refreshInstalledExtensions()
                        },
                        { _ ->
                            fileTemp.delete()
                            // Fallback ke instalasi langsung URL jika URI file lokal ditolak
                            installDirectlyViaGecko(trimmed, addonIdOrLabel)
                        }
                    )
                }
            } catch (_: Exception) {
                tempXpiFile?.delete()
                withContext(Dispatchers.Main) {
                    installDirectlyViaGecko(trimmed, addonIdOrLabel)
                }
            }
        }
    }

    private fun installDirectlyViaGecko(trimmedUrl: String, addonIdOrLabel: String) {
        _installProgressPercent.value = 65
        _statusMessage.value = "Memasang ekstensi langsung... 65%"
        try {
            controller.install(trimmedUrl).accept(
                { installedExt ->
                    _installProgressPercent.value = 100
                    val extName = installedExt?.metaData?.name ?: addonIdOrLabel
                    _statusMessage.value = "Ekstensi $extName berhasil dipasang ke Zenvora (100%)!"
                    if (installedExt != null) {
                        controller.setAllowedInPrivateBrowsing(installedExt, true)
                    }
                    _installingAddonId.value = null
                    _installProgressPercent.value = null
                    refreshInstalledExtensions()
                },
                { throwable ->
                    _installingAddonId.value = null
                    _installProgressPercent.value = null
                    val msg = throwable?.message ?: "Periksa koneksi internet atau tautan .xpi"
                    _statusMessage.value = "Gagal memasang ekstensi: $msg"
                    refreshInstalledExtensions()
                }
            )
        } catch (e: Exception) {
            _installingAddonId.value = null
            _installProgressPercent.value = null
            _statusMessage.value = "Gagal memasang ekstensi: ${e.message}"
        }
    }

    fun toggleExtension(info: InstalledExtensionInfo, enable: Boolean) {
        try {
            val result = if (enable) {
                controller.enable(info.rawExtension, WebExtensionController.EnableSource.USER)
            } else {
                controller.disable(info.rawExtension, WebExtensionController.EnableSource.USER)
            }
            result.accept(
                { _ ->
                    _statusMessage.value = if (enable) {
                        "${info.name} diaktifkan"
                    } else {
                        "${info.name} dinonaktifkan"
                    }
                    refreshInstalledExtensions()
                },
                { _ ->
                    refreshInstalledExtensions()
                }
            )
        } catch (_: Exception) {
            refreshInstalledExtensions()
        }
    }

    fun uninstallExtension(info: InstalledExtensionInfo) {
        if (info.isBuiltIn) return
        try {
            controller.uninstall(info.rawExtension).accept(
                { _ ->
                    _statusMessage.value = "Ekstensi ${info.name} telah dihapus"
                    refreshInstalledExtensions()
                },
                { _ ->
                    refreshInstalledExtensions()
                }
            )
        } catch (_: Exception) {
            refreshInstalledExtensions()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
