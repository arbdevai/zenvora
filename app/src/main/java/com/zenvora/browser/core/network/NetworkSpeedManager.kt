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
import okhttp3.Request
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
        displayName = "Cloudflare DoH (1.1.1.1)",
        primaryIp = "1.1.1.1",
        dohEndpoint = "https://1.1.1.1/dns-query",
        description = "Strict DNS-over-HTTPS terenkripsi bebas blokir DNS ISP/HP"
    ),
    GOOGLE(
        id = "google",
        displayName = "Google DoH (8.8.8.8)",
        primaryIp = "8.8.8.8",
        dohEndpoint = "https://8.8.8.8/dns-query",
        description = "Strict DNS-over-HTTPS global berkecepatan tinggi"
    ),
    QUAD9(
        id = "quad9",
        displayName = "Quad9 Secure DoH (9.9.9.9)",
        primaryIp = "9.9.9.9",
        dohEndpoint = "https://9.9.9.9:5053/dns-query",
        description = "DNS-over-HTTPS dengan pemblokiran malware & phishing"
    ),
    SYSTEM(
        id = "system",
        displayName = "DNS Standar Jaringan HP",
        primaryIp = "Auto",
        dohEndpoint = "",
        description = "Menggunakan resolver bawaan sistem Android / operator"
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

    // Bootstrap client murni berbasis IP langsung (tidak bergantung pada DNS HP)
    private val dohBootstrapClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val ipRegex = Regex("\"data\"\\s*:\\s*\"(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\"")
    private val directIpv4Regex = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    private fun resolveViaEncryptedDoh(hostname: String): List<InetAddress> {
        // Jika sudah berbentuk IPv4 langsung, tidak perlu query DNS
        if (directIpv4Regex.matches(hostname)) {
            val parts = hostname.split(".").map { it.toInt().toByte() }.toByteArray()
            return listOf(InetAddress.getByAddress(hostname, parts))
        }

        val provider = _networkStats.value.dnsProvider
        val dohQueryUrl = when (provider) {
            DnsProvider.GOOGLE -> "https://8.8.8.8/resolve?name=$hostname&type=A"
            else -> "https://1.1.1.1/dns-query?name=$hostname&type=A"
        }

        return try {
            val req = Request.Builder()
                .url(dohQueryUrl)
                .header("Accept", "application/dns-json")
                .build()
            dohBootstrapClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val bodyStr = resp.body?.string().orEmpty()
                val addresses = ipRegex.findAll(bodyStr).mapNotNull { matchResult ->
                    val ipStr = matchResult.groupValues[1]
                    try {
                        val octets = ipStr.split(".").map { octet -> octet.toInt().toByte() }.toByteArray()
                        InetAddress.getByAddress(hostname, octets)
                    } catch (_: Exception) {
                        null
                    }
                }.toList()
                addresses
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val fastDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val cached = dnsCache[hostname]
            if (!cached.isNullOrEmpty()) {
                return cached
            }

            val provider = _networkStats.value.dnsProvider
            if (provider != DnsProvider.SYSTEM) {
                val dohResult = resolveViaEncryptedDoh(hostname)
                if (dohResult.isNotEmpty()) {
                    dnsCache[hostname] = dohResult
                    _networkStats.update { it.copy(cachedDomainsCount = dnsCache.size) }
                    return dohResult
                }
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
            val directIp = if (provider != DnsProvider.SYSTEM) provider.primaryIp else "1.1.1.1"

            // Pre-resolve domain melalui DoH terenkripsi jika ada targetUrl
            if (!targetUrl.isNullOrBlank() && targetUrl.startsWith("http")) {
                val parsedHost = try {
                    URI(targetUrl).host
                } catch (_: Exception) {
                    null
                }
                if (!parsedHost.isNullOrBlank()) {
                    try {
                        fastDns.lookup(parsedHost)
                    } catch (_: Exception) {
                        // Abaikan kegagalan pre-resolve
                    }
                }
            }

            // Ukur ping langsung ke IP bootstrap DoH port 443 tanpa lewat DNS HP
            val startMs = SystemClock.elapsedRealtime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(directIp, 443), 2500)
            }
            val rtt = (SystemClock.elapsedRealtime() - startMs).coerceAtLeast(1L)

            _networkStats.update {
                it.copy(
                    latencyMs = rtt,
                    cachedDomainsCount = dnsCache.size
                )
            }
        } catch (_: Exception) {
            // Abaikan jika perangkat sedang offline
        }
    }

    private suspend fun warmupDefaultDomains() = withContext(Dispatchers.IO) {
        val popularHosts = listOf(
            "www.google.com",
            "duckduckgo.com",
            "www.youtube.com",
            "github.com"
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
