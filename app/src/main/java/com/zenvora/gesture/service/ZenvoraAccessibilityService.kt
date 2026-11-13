package com.zenvora.gesture.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.zenvora.gesture.core.engine.EngineRuntimeState
import com.zenvora.gesture.core.engine.EngineStatus
import com.zenvora.gesture.core.engine.HumanizedGestureBuilder
import com.zenvora.gesture.core.engine.ScreenBounds
import com.zenvora.gesture.data.model.ProfileMode
import com.zenvora.gesture.data.model.ProfileWithSteps
import com.zenvora.gesture.data.model.StepActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ZenvoraAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var executionJob: Job? = null
    private var currentProfile: ProfileWithSteps? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Window content retrieval is disabled; no event processing required.
    }

    override fun onInterrupt() {
        stopExecution()
    }

    override fun onDestroy() {
        stopExecution()
        _isServiceConnected.value = false
        if (instance === this) {
            instance = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    fun startProfile(profileWithSteps: ProfileWithSteps) {
        currentProfile = profileWithSteps
        executionJob?.cancel()
        _runtimeState.value = EngineRuntimeState(
            status = EngineStatus.RUNNING,
            activeProfileId = profileWithSteps.profile.id,
            activeProfileName = profileWithSteps.profile.name,
            cycleCount = 0,
            nextActionInMs = 0L,
            lastMessage = null
        )
        executionJob = serviceScope.launch {
            runLoop(profileWithSteps)
        }
    }

    fun pauseExecution() {
        if (_runtimeState.value.status == EngineStatus.RUNNING) {
            _runtimeState.value = _runtimeState.value.copy(status = EngineStatus.PAUSED)
        }
    }

    fun resumeExecution() {
        if (_runtimeState.value.status == EngineStatus.PAUSED) {
            _runtimeState.value = _runtimeState.value.copy(status = EngineStatus.RUNNING)
        }
    }

    fun stopExecution() {
        executionJob?.cancel()
        executionJob = null
        val prev = _runtimeState.value
        _runtimeState.value = prev.copy(
            status = EngineStatus.IDLE,
            nextActionInMs = 0L
        )
    }

    private suspend fun runLoop(profileWithSteps: ProfileWithSteps) {
        val profile = profileWithSteps.profile
        val steps = profileWithSteps.orderedSteps
        var cycles = 0

        // Short initial delay so floating button touch settles before first gesture
        delay(600L)

        while (serviceScope.isActive) {
            while (_runtimeState.value.status == EngineStatus.PAUSED) {
                delay(200L)
            }

            if (profile.repeatCount > 0 && cycles >= profile.repeatCount) {
                _runtimeState.value = _runtimeState.value.copy(
                    status = EngineStatus.IDLE,
                    nextActionInMs = 0L,
                    lastMessage = "Selesai (${cycles} siklus)"
                )
                break
            }

            val bounds = getScreenBounds()
            when (profile.mode) {
                ProfileMode.AUTO_SCROLL -> {
                    val gesture = HumanizedGestureBuilder.buildDirectionalScroll(
                        bounds = bounds,
                        direction = profile.scrollDirection,
                        distancePercent = profile.scrollDistancePercent,
                        jitterPx = profile.humanizedJitterPx,
                        durationMs = profile.scrollDurationMs,
                        useBezierCurve = profile.useBezierCurve
                    )
                    dispatchGestureSuspend(gesture)
                }

                ProfileMode.SINGLE_TAP -> {
                    val target = steps.firstOrNull()
                    val normX = target?.x ?: 0.5f
                    val normY = target?.y ?: 0.55f
                    val dur = target?.durationMs ?: 80L
                    val gesture = HumanizedGestureBuilder.buildTap(
                        bounds = bounds,
                        normalizedX = normX,
                        normalizedY = normY,
                        jitterPx = profile.humanizedJitterPx,
                        baseDurationMs = dur
                    )
                    dispatchGestureSuspend(gesture)
                }

                ProfileMode.MACRO_SEQUENCE -> {
                    for (step in steps) {
                        while (_runtimeState.value.status == EngineStatus.PAUSED) {
                            delay(200L)
                        }
                        when (step.actionType) {
                            StepActionType.TAP -> {
                                val gesture = HumanizedGestureBuilder.buildTap(
                                    bounds = bounds,
                                    normalizedX = step.x,
                                    normalizedY = step.y,
                                    jitterPx = profile.humanizedJitterPx,
                                    baseDurationMs = step.durationMs
                                )
                                dispatchGestureSuspend(gesture)
                            }
                            StepActionType.SWIPE -> {
                                val gesture = HumanizedGestureBuilder.buildSwipe(
                                    bounds = bounds,
                                    startNormX = step.x,
                                    startNormY = step.y,
                                    endNormX = step.endX,
                                    endNormY = step.endY,
                                    jitterPx = profile.humanizedJitterPx,
                                    baseDurationMs = step.durationMs.coerceAtLeast(180L),
                                    useBezierCurve = profile.useBezierCurve
                                )
                                dispatchGestureSuspend(gesture)
                            }
                            StepActionType.WAIT -> Unit
                        }
                        if (step.delayAfterMs > 0L) {
                            delayWithPause(step.delayAfterMs, updateCountdown = false)
                        }
                    }
                }
            }

            cycles += 1
            val waitMs = HumanizedGestureBuilder.randomizeInterval(
                baseIntervalMs = profile.intervalMs,
                varianceMs = profile.intervalVarianceMs
            )
            _runtimeState.value = _runtimeState.value.copy(
                cycleCount = cycles,
                nextActionInMs = waitMs
            )

            delayWithPause(waitMs, updateCountdown = true)
        }
    }

    private suspend fun delayWithPause(totalMs: Long, updateCountdown: Boolean) {
        var remaining = totalMs
        val stepMs = 250L
        while (remaining > 0L && serviceScope.isActive) {
            if (_runtimeState.value.status == EngineStatus.PAUSED) {
                delay(200L)
                continue
            }
            val chunk = minOf(stepMs, remaining)
            delay(chunk)
            remaining -= chunk
            if (updateCountdown) {
                _runtimeState.value = _runtimeState.value.copy(nextActionInMs = remaining)
            }
        }
        if (updateCountdown) {
            _runtimeState.value = _runtimeState.value.copy(nextActionInMs = 0L)
        }
    }

    private suspend fun dispatchGestureSuspend(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
                null
            )
            if (!dispatched && cont.isActive) {
                cont.resume(false)
            }
        }

    private fun getScreenBounds(): ScreenBounds {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            ScreenBounds(bounds.width().coerceAtLeast(1), bounds.height().coerceAtLeast(1))
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            ScreenBounds(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1))
        }
    }

    companion object {
        @Volatile
        var instance: ZenvoraAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _runtimeState = MutableStateFlow(EngineRuntimeState())
        val runtimeState: StateFlow<EngineRuntimeState> = _runtimeState.asStateFlow()
    }
}
