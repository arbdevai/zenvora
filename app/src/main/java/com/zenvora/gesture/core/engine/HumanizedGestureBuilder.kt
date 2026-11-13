package com.zenvora.gesture.core.engine

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import com.zenvora.gesture.data.model.ScrollDirection
import kotlin.math.hypot
import kotlin.random.Random

data class ScreenBounds(
    val widthPx: Int,
    val heightPx: Int
)

object HumanizedGestureBuilder {

    fun buildTap(
        bounds: ScreenBounds,
        normalizedX: Float,
        normalizedY: Float,
        jitterPx: Int,
        baseDurationMs: Long = 80L
    ): GestureDescription {
        val rawX = normalizedX * bounds.widthPx
        val rawY = normalizedY * bounds.heightPx
        val x = applyJitter(rawX, jitterPx, 12f, (bounds.widthPx - 12).toFloat())
        val y = applyJitter(rawY, jitterPx, 12f, (bounds.heightPx - 12).toFloat())

        val path = Path().apply {
            moveTo(x, y)
            // Micro-drift simulates natural finger contact
            val driftX = (x + Random.nextFloat() * 2f - 1f).coerceIn(8f, (bounds.widthPx - 8).toFloat())
            val driftY = (y + Random.nextFloat() * 2f - 1f).coerceIn(8f, (bounds.heightPx - 8).toFloat())
            lineTo(driftX, driftY)
        }

        val duration = randomizeDuration(baseDurationMs.coerceIn(45L, 250L), 0.25)
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun buildDirectionalScroll(
        bounds: ScreenBounds,
        direction: ScrollDirection,
        distancePercent: Int,
        jitterPx: Int,
        durationMs: Long,
        useBezierCurve: Boolean
    ): GestureDescription {
        val fraction = (distancePercent.coerceIn(15, 85)) / 100f
        val halfSpan = fraction / 2f

        val (startNormX, startNormY, endNormX, endNormY) = when (direction) {
            ScrollDirection.DOWN -> Quad(0.50f, (0.52f + halfSpan).coerceAtMost(0.88f), 0.50f, (0.52f - halfSpan).coerceAtLeast(0.14f))
            ScrollDirection.UP -> Quad(0.50f, (0.48f - halfSpan).coerceAtLeast(0.14f), 0.50f, (0.48f + halfSpan).coerceAtMost(0.88f))
            ScrollDirection.LEFT -> Quad((0.52f + halfSpan).coerceAtMost(0.90f), 0.50f, (0.52f - halfSpan).coerceAtLeast(0.10f), 0.50f)
            ScrollDirection.RIGHT -> Quad((0.48f - halfSpan).coerceAtLeast(0.10f), 0.50f, (0.48f + halfSpan).coerceAtMost(0.90f), 0.50f)
        }

        return buildSwipe(
            bounds = bounds,
            startNormX = startNormX,
            startNormY = startNormY,
            endNormX = endNormX,
            endNormY = endNormY,
            jitterPx = jitterPx,
            baseDurationMs = durationMs,
            useBezierCurve = useBezierCurve
        )
    }

    fun buildSwipe(
        bounds: ScreenBounds,
        startNormX: Float,
        startNormY: Float,
        endNormX: Float,
        endNormY: Float,
        jitterPx: Int,
        baseDurationMs: Long,
        useBezierCurve: Boolean
    ): GestureDescription {
        val sx = applyJitter(startNormX * bounds.widthPx, jitterPx, 16f, (bounds.widthPx - 16).toFloat())
        val sy = applyJitter(startNormY * bounds.heightPx, jitterPx, 16f, (bounds.heightPx - 16).toFloat())
        val ex = applyJitter(endNormX * bounds.widthPx, jitterPx, 16f, (bounds.widthPx - 16).toFloat())
        val ey = applyJitter(endNormY * bounds.heightPx, jitterPx, 16f, (bounds.heightPx - 16).toFloat())

        val path = Path().apply {
            moveTo(sx, sy)
            if (useBezierCurve) {
                val dx = ex - sx
                val dy = ey - sy
                val len = hypot(dx, dy).coerceAtLeast(1f)
                val perpX = -dy / len
                val perpY = dx / len
                val arcOffset = Random.nextFloat() * 34f - 17f
                val ctrlX = ((sx + ex) / 2f + perpX * arcOffset).coerceIn(12f, (bounds.widthPx - 12).toFloat())
                val ctrlY = ((sy + ey) / 2f + perpY * arcOffset).coerceIn(12f, (bounds.heightPx - 12).toFloat())
                quadTo(ctrlX, ctrlY, ex, ey)
            } else {
                lineTo(ex, ey)
            }
        }

        val duration = randomizeDuration(baseDurationMs.coerceIn(120L, 2_500L), 0.18)
        val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun randomizeInterval(baseIntervalMs: Long, varianceMs: Long): Long {
        if (varianceMs <= 0L) return baseIntervalMs.coerceAtLeast(100L)
        val delta = Random.nextLong(-varianceMs, varianceMs + 1L)
        return (baseIntervalMs + delta).coerceAtLeast(100L)
    }

    private fun applyJitter(value: Float, jitterPx: Int, minVal: Float, maxVal: Float): Float {
        if (jitterPx <= 0) return value.coerceIn(minVal, maxVal)
        val offset = (Random.nextFloat() * 2f - 1f) * jitterPx
        return (value + offset).coerceIn(minVal, maxVal)
    }

    private fun randomizeDuration(baseMs: Long, ratio: Double): Long {
        val delta = (baseMs * ratio).toLong().coerceAtLeast(1L)
        return (baseMs + Random.nextLong(-delta, delta + 1L)).coerceAtLeast(40L)
    }

    private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)
}
