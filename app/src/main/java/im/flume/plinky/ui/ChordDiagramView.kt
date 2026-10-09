package im.flume.plinky.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import im.flume.plinky.Chord
import im.flume.plinky.R

class ChordDiagramView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var chord: Chord? = null

    private val ink = ContextCompat.getColor(context, R.color.ink)
    private val inkSoft = ContextCompat.getColor(context, R.color.ink_soft)
    private val coral = ContextCompat.getColor(context, R.color.coral)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val stringNames = arrayOf("G", "C", "E", "A")

    fun setChord(c: Chord) {
        chord = c
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val c = chord ?: return
        val w = width.toFloat()
        val h = height.toFloat()

        // Chord name
        textPaint.color = ink
        textPaint.isFakeBoldText = true
        textPaint.textSize = h * 0.14f
        canvas.drawText(c.name, w / 2f, h * 0.13f, textPaint)
        textPaint.isFakeBoldText = false

        val numFrets = 5
        val top = h * 0.24f
        val bottom = h * 0.88f
        val left = w * 0.18f
        val right = w * 0.82f
        val fretGap = (bottom - top) / numFrets
        val stringGap = (right - left) / 3f

        // Nut
        paint.color = ink
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 12f
        canvas.drawLine(left - 6f, top, right + 6f, top, paint)

        // Frets
        paint.strokeWidth = 4f
        paint.color = inkSoft
        for (f in 1..numFrets) {
            val y = top + f * fretGap
            canvas.drawLine(left, y, right, y, paint)
        }

        // Strings
        paint.color = ink
        paint.strokeWidth = 4f
        for (s in 0..3) {
            val x = left + s * stringGap
            canvas.drawLine(x, top, x, bottom, paint)
        }

        // Dots & open markers
        for (s in 0..3) {
            val x = left + s * stringGap
            val fret = c.frets[s]
            if (fret == 0) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                paint.color = inkSoft
                canvas.drawCircle(x, top - fretGap * 0.42f, fretGap * 0.2f, paint)
            } else {
                paint.style = Paint.Style.FILL
                paint.color = coral
                val y = top + (fret - 0.5f) * fretGap
                canvas.drawCircle(x, y, fretGap * 0.32f, paint)
            }
        }

        // String labels
        textPaint.textSize = h * 0.055f
        textPaint.color = inkSoft
        for (s in 0..3) {
            val x = left + s * stringGap
            canvas.drawText(stringNames[s], x, h * 0.975f, textPaint)
        }
    }
}
