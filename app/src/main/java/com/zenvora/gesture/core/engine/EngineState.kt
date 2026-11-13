package com.zenvora.gesture.core.engine

enum class EngineStatus(val label: String) {
    IDLE("Siap"),
    RUNNING("Berjalan"),
    PAUSED("Dijeda"),
    ERROR("Kendala")
}

data class EngineRuntimeState(
    val status: EngineStatus = EngineStatus.IDLE,
    val activeProfileId: Long = 0L,
    val activeProfileName: String = "Belum ada profil",
    val cycleCount: Int = 0,
    val nextActionInMs: Long = 0L,
    val lastMessage: String? = null
)
