package com.zenvora.browser.data.model

enum class SecurityStatus {
    SECURE,
    INSECURE,
    INTERNAL,
    UNKNOWN
}

data class SecurityInfo(
    val status: SecurityStatus = SecurityStatus.UNKNOWN,
    val host: String = "",
    val scheme: String = "",
    val certificateSubject: String? = null,
    val certificateIssuer: String? = null
)
