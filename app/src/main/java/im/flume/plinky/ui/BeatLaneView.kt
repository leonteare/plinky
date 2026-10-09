package im.flume.plinky.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import im.flume.plinky.R
import kotlin.math.min

/**
 * One bar of beats as big circles. Highlighted beats are the ones the player
 * must strum. Flashes show hit grades (green = perfect, amber = good,
 * red = miss).
 */
class BeatLaneView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var count = 4
    private var highlights = BooleanArray(4)
    private var current = -1

    private class Flash(val beat: Int, val color: Int, val at: Long)

    private val flashes = mutableListOf<Flash>()

    private val coral = ContextCompat.getColor(context, R.color.coral)
    private val teal = ContextCompat.getColor(context, R.color.teal)
    private val inkSoft = ContextCompat.getColor(context, R.color.ink_soft)

    val colorPerfect = ContextCompat.getColor(context, R.color.good_green)
    val colorGood = ContextCompat.getColor(context, R.color.sun)
    val colorMiss = ContextCompat.getColor(context, R.color.miss_red)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun configure(beats: Int) {
        count = beats
        highlights = BooleanArray(beats)
        current = -1
        flashes.clear()
        invalidate()
    }

    fun setPattern(h: BooleanArray) {
        highlights = h
        invalidate()
    }

    fun setCurrent(beat: Int) {
        current = beat
        invalidate()
    }

    fun flash(beat: Int, color: Int) {
        flashes.add(Flash(beat, color, SystemClock.uptimeMillis()))
        postInvalidateOnAnimation()
    }

    fun reset() {
        current = -1
        flashes.clear()
        highlights = BooleanArray(count)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val slot = w / count
        val base = min(h * 0.3f, slot * 0.32f)
        val now = SystemClock.uptimeMillis()
        flashes.removeAll { now - it.at > 450 }

        for (i in 0 until count) {
            val cx = slot * (i + 0.5f)
            val cy = h / 2f

            if (highlights.size > i && highlights[i]) {
                paint.style = Paint.Style.FILL
                paint.color = coral
                canvas.drawCircle(cx, cy, base, paint)
            } else {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = inkSoft
                canvas.drawCircle(cx, cy, base, paint)
            }

            if (i == current) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
                paint.color = teal
                canvas.drawCircle(cx, cy, base * 1.3f, paint)
            }
        }

        for (f in flashes) {
            val k = 1f - (now - f.at) / 450f
            val cx = slot * (f.beat + 0.5f)
            paint.style = Paint.Style.FILL
            paint.color = f.color
            paint.alpha = (200 * k).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, h / 2f, base * (1.1f + 0.5f * (1f - k)), paint)
            paint.alpha = 255
        }
        if (flashes.isNotEmpty()) postInvalidateOnAnimation()
    }
}
