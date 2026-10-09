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

class BeatDotsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var count = 4
    private var current = -1
    private var pulseAt = 0L

    private val coral = ContextCompat.getColor(context, R.color.coral)
    private val sun = ContextCompat.getColor(context, R.color.sun)
    private val inkSoft = ContextCompat.getColor(context, R.color.ink_soft)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun configure(beats: Int) {
        count = beats
        current = -1
        invalidate()
    }

    fun pulse(beat: Int) {
        current = beat
        pulseAt = SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    fun reset() {
        current = -1
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val slot = w / count
        val base = min(h * 0.28f, slot * 0.3f)
        val elapsed = SystemClock.uptimeMillis() - pulseAt
        var animating = false

        for (i in 0 until count) {
            val cx = slot * (i + 0.5f)
            val cy = h / 2f
            if (i == current && elapsed < 260) {
                val k = 1f - elapsed / 260f
                val r = base * (1f + 0.45f * k)
                paint.style = Paint.Style.FILL
                paint.color = if (i == 0) sun else coral
                canvas.drawCircle(cx, cy, r, paint)
                animating = true
            } else if (i == current) {
                paint.style = Paint.Style.FILL
                paint.color = if (i == 0) sun else coral
                canvas.drawCircle(cx, cy, base, paint)
            } else {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = inkSoft
                canvas.drawCircle(cx, cy, base, paint)
            }
        }
        if (animating) postInvalidateOnAnimation()
    }
}
