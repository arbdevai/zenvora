package com.zenvora.browser.core.extensions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

data class InstalledExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val isEnabled: Boolean,
    val isBuiltIn: Boolean,
    val rawExtension: WebExtension
)

data class RecommendedAddon(
    val id: String,
    val name: String,
    val summary: String,
    val xpiDownloadUrl: String,
    val storePageUrl: String
)

class ExtensionManager(private val runtime: GeckoRuntime) {

    companion object {
        const val BUILTIN_SHIELD_ID = "zenvora-shield@zenvora.browser"
        const val AMO_STORE_HOME_URL = "https://addons.mozilla.org/id/android/"

        val RECOMMENDED_ADDONS = listOf(
            RecommendedAddon(
                id = "uBlock0@raymondhill.net",
                name = "uBlock Origin",
                summary = "Pemblokir iklan, pop-under, dan anti-adblock paling ampuh dengan filter EasyList & ABPindo penuh (Setara/Lebih Kuat dari Brave).",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/id/android/addon/ublock-origin/"
            ),
            RecommendedAddon(
                id = "addon@darkreader.org",
                name = "Dark Reader",
                summary = "Ubah seluruh tampilan situs web terang menjadi tema gelap OLED yang nyaman di mata.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/darkreader/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/id/android/addon/darkreader/"
            ),
            RecommendedAddon(
                id = "sponsorBlocker@ajay.app",
                name = "SponsorBlock for YouTube",
                summary = "Lewati otomatis segmen sponsor, intro, dan promosi di dalam video YouTube.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/sponsorblock/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/id/android/addon/sponsorblock/"
            ),
            RecommendedAddon(
                id = "firefox@tampermonkey.net",
                name = "Tampermonkey",
                summary = "Pengelola userscript populer untuk menjalankan script kustom dan bypass situs modern.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/tampermonkey/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/id/android/addon/tampermonkey/"
            ),
            RecommendedAddon(
                id = "{74145f27-f039-47ce-a470-a662b129930a}",
                name = "ClearURLs",
                summary = "Hapus otomatis parameter pelacakan (utm, fbclid,igs) dari alamat URL situs.",
                xpiDownloadUrl = "https://addons.mozilla.org/firefox/downloads/latest/clearurls/latest.xpi",
                storePageUrl = "https://addons.mozilla.org/id/android/addon/clearurls/"
            )
        )
    }

    private val controller: WebExtensionController = runtime.webExtensionController

    private val _installedExtensions = MutableStateFlow<List<InstalledExtensionInfo>>(emptyList())
    val installedExtensions: StateFlow<List<InstalledExtensionInfo>> = _installedExtensions.asStateFlow()

    private val _installingAddonId = MutableStateFlow<String?>(null)
    val installingAddonId: StateFlow<String?> = _installingAddonId.asStateFlow()

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
                                ext.id == BUILTIN_SHIELD_ID -> "Perisai adblock bawaan Zenvora, anti-adblock defuser, & pembuka background play YouTube."
                                !meta?.description.isNullOrBlank() -> meta?.description.orEmpty()
                                else -> "Ekstensi WebExtension Mozilla GeckoView"
                            }
                            val versionStr = meta?.version?.ifBlank { "1.0" } ?: "1.0"
                            val enabled = meta?.enabled ?: true

                            InstalledExtensionInfo(
                                id = ext.id,
                                name = displayName,
                                version = versionStr,
                                description = displayDesc,
                                isEnabled = enabled,
                                isBuiltIn = isBuiltIn,
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
        _statusMessage.value = "Mengunduh & memasang ekstensi..."

        try {
            controller.install(trimmed).accept(
                { installedExt ->
                    _installingAddonId.value = null
                    val extName = installedExt?.metaData?.name ?: addonIdOrLabel
                    _statusMessage.value = "Ekstensi $extName berhasil dipasang dan aktif!"
                    if (installedExt != null) {
                        controller.setAllowedInPrivateBrowsing(installedExt, true)
                    }
                    refreshInstalledExtensions()
                },
                { throwable ->
                    _installingAddonId.value = null
                    val msg = throwable?.message ?: "Periksa koneksi internet atau URL .xpi"
                    _statusMessage.value = "Gagal memasang ekstensi: $msg"
                    refreshInstalledExtensions()
                }
            )
        } catch (e: Exception) {
            _installingAddonId.value = null
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
