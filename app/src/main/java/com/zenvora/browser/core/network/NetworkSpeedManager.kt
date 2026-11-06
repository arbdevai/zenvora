package com.zenvora.browser.core.network

import android.net.TrafficStats
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class DnsProvider(
    val id: String,
    val displayName: String,
    val primaryIp: String,
    val dohEndpoint: String,
    val description: String
) {
    CLOUDFLARE(
        id = "cloudflare",
        displayName = "Cloudflare Warp DNS (1.1.1.1)",
        primaryIp = "1.1.1.1",
        dohEndpoint = "https://cloudflare-dns.com/dns-query",
        description = "Resolusi DNS tercepat dengan latensi ultra-rendah"
    ),
    GOOGLE(
        id = "google",
        displayName = "Google High-Speed DNS (8.8.8.8)",
        primaryIp = "8.8.8.8",
        dohEndpoint = "https://dns.google/dns-query",
        description = "Infrastruktur global stabil dengan throughput tinggi"
    ),
    QUAD9(
        id = "quad9",
        displayName = "Quad9 Secure DNS (9.9.9.9)",
        primaryIp = "9.9.9.9",
        dohEndpoint = "https://dns.quad9.net/dns-query",
        description = "DNS cepat dengan proteksi domain berbahaya"
    ),
    SYSTEM(
        id = "system",
        displayName = "DNS Standar Jaringan",
        primaryIp = "Auto",
        dohEndpoint = "",
        description = "Gunakan resolver bawaan operator seluler atau Wi-Fi"
    );

    companion object {
        fun fromId(id: String): DnsProvider {
            return entries.firstOrNull { it.id == id } ?: CLOUDFLARE
        }
    }
}

data class NetworkStats(
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val peakDownloadBytesPerSec: Long = 0L,
    val sessionRxBytes: Long = 0L,
    val sessionTxBytes: Long = 0L,
    val latencyMs: Long = 0L,
    val cachedDomainsCount: Int = 0,
    val isTurboEnabled: Boolean = true,
    val dnsProvider: DnsProvider = DnsProvider.CLOUDFLARE
) {
    val formattedDownloadSpeed: String
        get() = formatSpeed(downloadBytesPerSec)

    val formattedUploadSpeed: String
        get() = formatSpeed(uploadBytesPerSec)

    val formattedPeakSpeed: String
        get() = formatSpeed(peakDownloadBytesPerSec)

    val formattedSessionTotal: String
        get() = formatBytes(sessionRxBytes + sessionTxBytes)

    companion object {
        fun formatSpeed(bytesPerSec: Long): String {
            return when {
                bytesPerSec < 1024L -> "${bytesPerSec} B/s"
                bytesPerSec < 1024L * 1024L -> String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024.0)
                else -> String.format(Locale.US, "%.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
            }
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024L -> "$bytes B"
                bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
                bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
                else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}

class NetworkSpeedManager {

    private val dnsCache = ConcurrentHashMap<String, List<InetAddress>>()

    private val _networkStats = MutableStateFlow(NetworkStats())
    val networkStats: StateFlow<NetworkStats> = _networkStats.asStateFlow()

    private var monitorJob: Job? = null

    private val fastDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val cached = dnsCache[hostname]
            if (!cached.isNullOrEmpty()) {
                return cached
            }
            val resolved = Dns.SYSTEM.lookup(hostname)
            if (resolved.isNotEmpty()) {
                dnsCache[hostname] = resolved
                _networkStats.update { it.copy(cachedDomainsCount = dnsCache.size) }
            }
            return resolved
        }
    }

    val highSpeedHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 16
            }
        )
        .dns(fastDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    fun startMonitoring(scope: CoroutineScope) {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch(Dispatchers.Default) {
            val uid = Process.myUid()
            var prevRx = readRxBytes(uid)
            var prevTx = readTxBytes(uid)
            val baseRx = prevRx
            val baseTx = prevTx
            var prevTime = SystemClock.elapsedRealtime()

            // Warmup awal ke DNS utama
            warmupDefaultDomains()

            while (isActive) {
                delay(1000L)
                val now = SystemClock.elapsedRealtime()
                val elapsedSec = ((now - prevTime) / 1000.0).coerceAtLeast(0.5)
                val currentRx = readRxBytes(uid)
                val currentTx = readTxBytes(uid)

                val deltaRx = (currentRx - prevRx).coerceAtLeast(0L)
                val deltaTx = (currentTx - prevTx).coerceAtLeast(0L)

                val rxPerSec = (deltaRx / elapsedSec).toLong()
                val txPerSec = (deltaTx / elapsedSec).toLong()

                prevRx = currentRx
                prevTx = currentTx
                prevTime = now

                _networkStats.update { current ->
                    val newPeak = maxOf(current.peakDownloadBytesPerSec, rxPerSec)
                    current.copy(
                        downloadBytesPerSec = rxPerSec,
                        uploadBytesPerSec = txPerSec,
                        peakDownloadBytesPerSec = newPeak,
                        sessionRxBytes = (currentRx - baseRx).coerceAtLeast(0L),
                        sessionTxBytes = (currentTx - baseTx).coerceAtLeast(0L),
                        cachedDomainsCount = dnsCache.size
                    )
                }
            }
        }
    }

    private fun readRxBytes(uid: Int): Long {
        val uidBytes = TrafficStats.getUidRxBytes(uid)
        if (uidBytes != TrafficStats.UNSUPPORTED.toLong() && uidBytes >= 0L) {
            return uidBytes
        }
        val total = TrafficStats.getTotalRxBytes()
        return if (total >= 0L) total else 0L
    }

    private fun readTxBytes(uid: Int): Long {
        val uidBytes = TrafficStats.getUidTxBytes(uid)
        if (uidBytes != TrafficStats.UNSUPPORTED.toLong() && uidBytes >= 0L) {
            return uidBytes
        }
        val total = TrafficStats.getTotalTxBytes()
        return if (total >= 0L) total else 0L
    }

    fun setTurboEnabled(enabled: Boolean) {
        _networkStats.update { it.copy(isTurboEnabled = enabled) }
    }

    fun setDnsProvider(provider: DnsProvider) {
        _networkStats.update { it.copy(dnsProvider = provider) }
    }

    suspend fun warmupAndMeasureLatency(targetUrl: String? = null) = withContext(Dispatchers.IO) {
        try {
            val provider = _networkStats.value.dnsProvider
            val hostToPing = when {
                !targetUrl.isNullOrBlank() && targetUrl != "about:blank" -> {
                    runCatching { URI(targetUrl).host }.getOrNull() ?: "1.1.1.1"
                }
                provider != DnsProvider.SYSTEM -> provider.primaryIp
                else -> "1.1.1.1"
            }

            // Pre-resolve DNS ke dalam in-memory cache
            val addresses = fastDns.lookup(hostToPing)
            val targetAddress = addresses.firstOrNull() ?: InetAddress.getByName("1.1.1.1")

            val startMs = SystemClock.elapsedRealtime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(targetAddress, 443), 2500)
            }
            val rtt = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(1L)

            _networkStats.update {
                it.copy(
                    latencyMs = rtt,
                    cachedDomainsCount = dnsCache.size
                )
            }
        } catch (_: Exception) {
            // Fallback ke DNS IP langsung jika port 443 host tujuan tertutup
            try {
                val startMs = SystemClock.elapsedRealtime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("1.1.1.1", 53), 2000)
                }
                val rtt = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(1L)
                _networkStats.update { it.copy(latencyMs = rtt) }
            } catch (_: Exception) {
                // Abaikan jika perangkat sedang offline
            }
        }
    }

    private suspend fun warmupDefaultDomains() = withContext(Dispatchers.IO) {
        val popularHosts = listOf(
            "www.google.com",
            "duckduckgo.com",
            "www.youtube.com",
            "github.com",
            "cloudflare-dns.com"
        )
        popularHosts.forEach { host ->
            runCatching { fastDns.lookup(host) }
        }
        warmupAndMeasureLatency()
    }

    fun clearDnsCache() {
        dnsCache.clear()
        _networkStats.update { it.copy(cachedDomainsCount = 0) }
    }
}
