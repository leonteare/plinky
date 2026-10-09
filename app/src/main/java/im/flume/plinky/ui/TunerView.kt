package im.flume.plinky.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import im.flume.plinky.R
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.min

class TunerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class UkeString(val name: String, val freq: Float)

    private val strings = listOf(
        UkeString("G", 392.00f),
        UkeString("C", 261.63f),
        UkeString("E", 329.63f),
        UkeString("A", 440.00f)
    )

    private var noteName = "–"
    private var cents = 0f
    private var freq = 0f
    private var active = false

    private val ink = ContextCompat.getColor(context, R.color.ink)
    private val inkSoft = ContextCompat.getColor(context, R.color.ink_soft)
    private val coral = ContextCompat.getColor(context, R.color.coral)
    private val teal = ContextCompat.getColor(context, R.color.teal)
    private val green = ContextCompat.getColor(context, R.color.good_green)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val fadeOut = Runnable {
        active = false
        invalidate()
    }

    fun update(freqHz: Float) {
        var best = strings[0]
        var bestCents = centsBetween(freqHz, best.freq)
        for (s in strings) {
            val c = centsBetween(freqHz, s.freq)
            if (abs(c) < abs(bestCents)) {
                best = s
                bestCents = c
            }
        }
        // Ignore wildly off readings (harmonics of something else entirely)
        if (abs(bestCents) > 120f) return

        if (best.name == noteName && active) {
            cents = 0.65f * cents + 0.35f * bestCents
        } else {
            cents = bestCents
        }
        noteName = best.name
        freq = freqHz
        active = true
        removeCallbacks(fadeOut)
        postDelayed(fadeOut, 1200)
        invalidate()
    }

    private fun centsBetween(f: Float, target: Float): Float =
        (1200.0 * ln(f.toDouble() / target) / ln(2.0)).toFloat()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val inTune = active && abs(cents) <= 5f

        // Big note letter
        val noteY = h * 0.38f
        if (inTune) {
            paint.color = green
            paint.alpha = 40
            paint.style = Paint.Style.FILL
            canvas.drawCircle(w / 2f, noteY - h * 0.09f, min(w, h) * 0.22f, paint)
            paint.alpha = 255
        }
        if (active) {
            textPaint.color = if (inTune) green else ink
            textPaint.textSize = h * 0.28f
            textPaint.isFakeBoldText = true
            canvas.drawText(noteName, w / 2f, noteY, textPaint)
        } else {
            textPaint.color = inkSoft
            textPaint.textSize = h * 0.16f
            textPaint.isFakeBoldText = false
            canvas.drawText("♪", w / 2f, noteY, textPaint)
        }

        // Frequency readout
        textPaint.isFakeBoldText = false
        textPaint.textSize = h * 0.05f
        textPaint.color = inkSoft
        val freqLabel = if (active) String.format("%.1f Hz", freq) else "listening…"
        canvas.drawText(freqLabel, w / 2f, noteY + h * 0.09f, textPaint)

        // Cents scale
        val scaleY = h * 0.72f
        val left = w * 0.08f
        val right = w * 0.92f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = inkSoft
        canvas.drawLine(left, scaleY, right, scaleY, paint)
        for (t in -50..50 step 10) {
            val x = left + (t + 50) / 100f * (right - left)
            val tickH = if (t == 0) h * 0.045f else h * 0.025f
            paint.strokeWidth = if (t == 0) 5f else 3f
            paint.color = if (t == 0) teal else inkSoft
            canvas.drawLine(x, scaleY - tickH, x, scaleY + tickH, paint)
        }

        // Needle
        if (active) {
            val c = cents.coerceIn(-50f, 50f)
            val x = left + (c + 50) / 100f * (right - left)
            paint.style = Paint.Style.FILL
            paint.color = if (inTune) green else coral
            canvas.drawCircle(x, scaleY, h * 0.022f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawLine(x, scaleY - h * 0.07f, x, scaleY + h * 0.07f, paint)
        }

        // Hint
        textPaint.textSize = h * 0.055f
        textPaint.color = if (inTune) green else inkSoft
        val hint = when {
            !active -> ""
            inTune -> "in tune ✓"
            cents < -8 -> "tune up ↑"
            cents > 8 -> "tune down ↓"
            else -> "almost…"
        }
        canvas.drawText(hint, w / 2f, scaleY + h * 0.15f, textPaint)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(fadeOut)
        super.onDetachedFromWindow()
    }
}
