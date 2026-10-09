package im.flume.plinky.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Continuously reads the microphone, detecting strum onsets and (optionally)
 * pitch. Strum detection runs on a low-passed copy of the signal so the
 * metronome's high-pitched clicks coming out of the speaker don't register
 * as strums.
 */
class MicEngine {

    interface Listener {
        fun onLevel(rms: Float) {}
        fun onPitch(freqHz: Float) {}
        fun onStrum(atNanos: Long, energy: Float) {}
    }

    private val listeners = CopyOnWriteArraySet<Listener>()
    private val main = Handler(Looper.getMainLooper())
    private var thread: Thread? = null

    @Volatile private var running = false
    @Volatile var pitchEnabled = false

    fun addListener(l: Listener) = listeners.add(l)
    fun removeListener(l: Listener) = listeners.remove(l)

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun start(context: Context): Boolean {
        if (running) return true
        if (!hasPermission(context)) return false
        running = true
        thread = Thread({ loop() }, "plinky-mic").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        return true
    }

    fun stop() {
        running = false
        thread?.join(800)
        thread = null
    }

    private fun loop() {
        val sr = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sr,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                max(minBuf * 2, 8192)
            )
        } catch (e: Exception) {
            running = false
            return
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            running = false
            return
        }
        rec.startRecording()

        val frame = ShortArray(1024)
        val window = FloatArray(2048)
        var wFill = 0

        // Two cascaded one-pole low-pass filters, ~900 Hz corner.
        var lp1 = 0f
        var lp2 = 0f
        val lpA = 0.12f

        var prevLpRms = 0f
        var noise = 0.002f
        var lastStrum = 0L

        while (running) {
            val n = rec.read(frame, 0, frame.size)
            if (n <= 0) continue
            val now = System.nanoTime()

            var sum = 0f
            var lpSum = 0f
            for (i in 0 until n) {
                val v = frame[i] / 32768f
                sum += v * v
                lp1 += lpA * (v - lp1)
                lp2 += lpA * (lp1 - lp2)
                lpSum += lp2 * lp2
            }
            val rms = sqrt(sum / n)
            val lpRms = sqrt(lpSum / n)

            val threshold = max(0.015f, noise * 4f)
            if (lpRms > threshold && lpRms > prevLpRms * 2f &&
                now - lastStrum > 160_000_000L
            ) {
                lastStrum = now
                val e = lpRms
                main.post { listeners.forEach { it.onStrum(now, e) } }
            }
            if (lpRms < threshold) noise = 0.98f * noise + 0.02f * lpRms
            prevLpRms = lpRms

            main.post { listeners.forEach { it.onLevel(rms) } }

            if (pitchEnabled) {
                var idx = 0
                while (idx < n) {
                    val copy = minOf(n - idx, window.size - wFill)
                    for (j in 0 until copy) window[wFill + j] = frame[idx + j] / 32768f
                    wFill += copy
                    idx += copy
                    if (wFill == window.size) {
                        if (rms > 0.008f) {
                            val f = Dsp.detectPitch(window, sr)
                            if (f > 0) main.post { listeners.forEach { it.onPitch(f) } }
                        }
                        System.arraycopy(window, 1024, window, 0, 1024)
                        wFill = 1024
                    }
                }
            }
        }

        try { rec.stop() } catch (_: Exception) {}
        rec.release()
    }
}
