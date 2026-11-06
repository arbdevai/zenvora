package com.zenvora.browser.core.engine

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

enum class VideoCodecMode(
    val id: String,
    val displayName: String,
    val description: String
) {
    AUTO_HARDWARE(
        id = "auto_hardware",
        displayName = "Otomatis (Sesuai Chipset Hardware HP)",
        description = "Aktifkan hanya codec yang memiliki dekoder hardware fisik di HP ini agar 720p/1080p lancar 60fps"
    ),
    FORCE_H264(
        id = "force_h264",
        displayName = "Performa Maksimal (H.264 / AVC Hardware)",
        description = "Paksa stream video menggunakan H.264 paling ringan dan hemat baterai di semua situs"
    ),
    ALL_CODECS(
        id = "all_codecs",
        displayName = "Semua Codec (AV1 + VP9 + H.264)",
        description = "Aktifkan seluruh codec modern termasuk software decoder jika hardware tidak tersedia"
    );

    companion object {
        fun fromId(id: String): VideoCodecMode {
            return entries.firstOrNull { it.id == id } ?: AUTO_HARDWARE
        }
    }
}

data class HardwareVideoCapabilities(
    val hasHardwareH264: Boolean = true,
    val hasHardwareHevc: Boolean = false,
    val hasHardwareVp9: Boolean = false,
    val hasHardwareAv1: Boolean = false,
    val decoderNamesSummary: String = "H.264 (AVC)"
) {
    fun shouldEnableVp9(mode: VideoCodecMode): Boolean {
        return when (mode) {
            VideoCodecMode.AUTO_HARDWARE -> hasHardwareVp9
            VideoCodecMode.FORCE_H264 -> false
            VideoCodecMode.ALL_CODECS -> true
        }
    }

    fun shouldEnableAv1(mode: VideoCodecMode): Boolean {
        return when (mode) {
            VideoCodecMode.AUTO_HARDWARE -> hasHardwareAv1
            VideoCodecMode.FORCE_H264 -> false
            VideoCodecMode.ALL_CODECS -> true
        }
    }
}

object HardwareCodecDetector {

    @Volatile
    private var cachedCapabilities: HardwareVideoCapabilities? = null

    fun detectCapabilities(): HardwareVideoCapabilities {
        cachedCapabilities?.let { return it }

        var hwH264 = false
        var hwHevc = false
        var hwVp9 = false
        var hwAv1 = false

        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val codecInfos = codecList.codecInfos

            for (info in codecInfos) {
                if (info.isEncoder) continue
                if (!isTrueHardwareDecoder(info)) continue

                for (type in info.supportedTypes) {
                    when {
                        type.equals("video/avc", ignoreCase = true) -> hwH264 = true
                        type.equals("video/hevc", ignoreCase = true) -> hwHevc = true
                        type.equals("video/x-vnd.on2.vp9", ignoreCase = true) -> hwVp9 = true
                        type.equals("video/av01", ignoreCase = true) -> hwAv1 = true
                    }
                }
            }
        } catch (_: Exception) {
            hwH264 = true
        }

        if (!hwH264) {
            hwH264 = true
        }

        val supportedList = mutableListOf<String>()
        if (hwH264) supportedList.add("H.264")
        if (hwHevc) supportedList.add("HEVC")
        if (hwVp9) supportedList.add("VP9")
        if (hwAv1) supportedList.add("AV1")

        val result = HardwareVideoCapabilities(
            hasHardwareH264 = hwH264,
            hasHardwareHevc = hwHevc,
            hasHardwareVp9 = hwVp9,
            hasHardwareAv1 = hwAv1,
            decoderNamesSummary = supportedList.joinToString(" + ")
        )
        cachedCapabilities = result
        return result
    }

    private fun isTrueHardwareDecoder(info: MediaCodecInfo): Boolean {
        val name = info.name.lowercase()

        // Filter keluar seluruh software decoder bawaan Google / Android / FFmpeg / Gav1 / Dav1d / Libvpx
        if (
            name.startsWith("omx.google.") ||
            name.startsWith("c2.android.") ||
            name.startsWith("omx.ffmpeg.") ||
            name.startsWith("omx.sec.avc.sw.") ||
            name.contains(".sw.") ||
            name.contains("software") ||
            name.contains("gav1") ||
            name.contains("dav1d") ||
            name.contains("vpx")
        ) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isHardwareAccelerated && !info.isSoftwareOnly
        }

        // Fallback untuk Android 8.0 - 9.0 (API 26-28): cek prefix chip SoC fisik
        return name.startsWith("omx.mtk.") ||
            name.startsWith("omx.qcom.") ||
            name.startsWith("omx.exynos.") ||
            name.startsWith("omx.img.") ||
            name.startsWith("omx.hisi.") ||
            name.startsWith("omx.sprd.") ||
            name.startsWith("c2.mtk.") ||
            name.startsWith("c2.qti.") ||
            name.startsWith("c2.exynos.")
    }
}
