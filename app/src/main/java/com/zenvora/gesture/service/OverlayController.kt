package com.zenvora.gesture.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.zenvora.gesture.R
import com.zenvora.gesture.core.engine.EngineStatus
import com.zenvora.gesture.data.model.ProfileMode
import com.zenvora.gesture.data.model.ProfileWithSteps
import com.zenvora.gesture.data.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

class OverlayController(
    private val context: Context,
    private val repository: ProfileRepository,
    private val onCloseRequested: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controlView: LinearLayout? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private var playPauseBtn: ImageButton? = null
    private var stopBtn: ImageButton? = null
    private var targetsBtn: ImageButton? = null
    private var countdownText: TextView? = null

    private val targetViews = mutableListOf<View>()
    private var showTargetPins = false
    private var activeProfile: ProfileWithSteps? = null
    private var observerJob: Job? = null

    fun show() {
        if (controlView != null) return
        buildControlBar()
        startObserving()
    }

    fun hide() {
        observerJob?.cancel()
        observerJob = null
        clearTargetPins()
        controlView?.let {
            runCatching { windowManager.removeView(it) }
        }
        controlView = null
        scope.cancel()
    }

    private fun startObserving() {
        observerJob = scope.launch {
            launch {
                repository.observeActiveProfile().collectLatest { profile ->
                    activeProfile = profile
                    if (showTargetPins) {
                        rebuildTargetPins()
                    }
                }
            }
            launch {
                ZenvoraAccessibilityService.runtimeState.collectLatest { state ->
                    updateHudState(state.status, state.nextActionInMs, state.cycleCount)
                }
            }
        }
    }

    private fun updateHudState(status: EngineStatus, nextActionInMs: Long, cycleCount: Int) {
        val isRunning = status == EngineStatus.RUNNING
        val isPaused = status == EngineStatus.PAUSED
        playPauseBtn?.setImageResource(
            if (isRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        stopBtn?.visibility = if (isRunning || isPaused) View.VISIBLE else View.GONE

        countdownText?.text = when {
            isRunning && nextActionInMs > 0L -> "${(nextActionInMs + 999L) / 1000L}d"
            isRunning -> "#$cycleCount"
            isPaused -> "Jeda"
            else -> "Siap"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun buildControlBar() {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).roundToInt()

        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#121212"))
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), Color.parseColor("#2E2E2E"))
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = bg
            setPadding(dp(8), dp(10), dp(8), dp(10))
            elevation = dp(8).toFloat()
        }

        val dragHandle = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setColorFilter(Color.parseColor("#8E8E8E"))
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(20)).apply {
                bottomMargin = dp(6)
            }
        }

        val statusBadge = TextView(context).apply {
            text = "Siap"
            setTextColor(Color.parseColor("#E0E0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }
        }
        countdownText = statusBadge

        fun createIconButton(iconRes: Int, tintHex: String, onClick: () -> Unit): ImageButton {
            return ImageButton(context).apply {
                setImageResource(iconRes)
                setColorFilter(Color.parseColor(tintHex))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1E1E1E"))
                    cornerRadius = dp(16).toFloat()
                }
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    bottomMargin = dp(6)
                }
                setOnClickListener { onClick() }
            }
        }

        val btnPlayPause = createIconButton(android.R.drawable.ic_media_play, "#FFFFFF") {
            val service = ZenvoraAccessibilityService.instance ?: return@createIconButton
            val status = ZenvoraAccessibilityService.runtimeState.value.status
            when (status) {
                EngineStatus.RUNNING -> service.pauseExecution()
                EngineStatus.PAUSED -> service.resumeExecution()
                EngineStatus.IDLE, EngineStatus.ERROR -> {
                    activeProfile?.let { service.startProfile(it) }
                }
            }
        }
        playPauseBtn = btnPlayPause

        val btnStop = createIconButton(android.R.drawable.ic_menu_close_clear_cancel, "#E57373") {
            ZenvoraAccessibilityService.instance?.stopExecution()
        }.apply {
            visibility = View.GONE
        }
        stopBtn = btnStop

        val btnTargets = createIconButton(android.R.drawable.ic_menu_mylocation, "#BDBDBD") {
            showTargetPins = !showTargetPins
            if (showTargetPins) {
                rebuildTargetPins()
            } else {
                clearTargetPins()
            }
        }
        targetsBtn = btnTargets

        val btnClose = createIconButton(android.R.drawable.ic_delete, "#8E8E8E") {
            ZenvoraAccessibilityService.instance?.stopExecution()
            onCloseRequested()
        }.apply {
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = 0
        }

        container.addView(dragHandle)
        container.addView(statusBadge)
        container.addView(btnPlayPause)
        container.addView(btnStop)
        container.addView(btnTargets)
        container.addView(btnClose)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(180)
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).roundToInt()
                    params.y = initialY + (event.rawY - touchY).roundToInt()
                    runCatching { windowManager.updateViewLayout(container, params) }
                    true
                }
                else -> false
            }
        }

        controlView = container
        controlParams = params
        runCatching { windowManager.addView(container, params) }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun rebuildTargetPins() {
        clearTargetPins()
        val profile = activeProfile ?: return
        if (profile.profile.mode == ProfileMode.AUTO_SCROLL) return

        val (screenW, screenH) = getScreenSize()
        val density = context.resources.displayMetrics.density
        val sizePx = (42 * density).roundToInt()
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        profile.orderedSteps.forEachIndexed { index, step ->
            val pinView = TextView(context).apply {
                text = "${index + 1}"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#FFFFFF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#CC1E1E1E"))
                    setStroke((2 * density).roundToInt(), Color.parseColor("#FFFFFF"))
                }
            }

            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (step.x * screenW - sizePx / 2f).roundToInt()
                y = (step.y * screenH - sizePx / 2f).roundToInt()
            }

            var initialX = 0
            var initialY = 0
            var touchX = 0f
            var touchY = 0f

            pinView.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - touchX).roundToInt()
                        params.y = initialY + (event.rawY - touchY).roundToInt()
                        runCatching { windowManager.updateViewLayout(pinView, params) }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (abs(event.rawX - touchX) > 4f || abs(event.rawY - touchY) > 4f) {
                            val normX = (params.x + sizePx / 2f) / screenW.toFloat()
                            val normY = (params.y + sizePx / 2f) / screenH.toFloat()
                            scope.launch {
                                repository.updateStepCoordinates(step.id, normX, normY)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            targetViews.add(pinView)
            runCatching { windowManager.addView(pinView, params) }
        }
    }

    private fun clearTargetPins() {
        targetViews.forEach { view ->
            runCatching { windowManager.removeView(view) }
        }
        targetViews.clear()
    }

    private fun getScreenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width().coerceAtLeast(1) to bounds.height().coerceAtLeast(1)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels.coerceAtLeast(1) to metrics.heightPixels.coerceAtLeast(1)
        }
    }
}
