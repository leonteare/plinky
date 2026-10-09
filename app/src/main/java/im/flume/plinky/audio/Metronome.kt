package im.flume.plinky.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Streaming AudioTrack metronome. Clicks are written sample-accurately into
 * the output stream; beat callbacks report an estimate of when the click is
 * actually audible (write position minus playback head, converted to time).
 * Clicks are high-pitched sine bursts so MicEngine's low-passed strum
 * detector largely ignores them.
 */
class Metronome {

    interface BeatListener {
        fun onBeat(bar: Int, beat: Int, audibleAtNanos: Long)
    }

    @Volatile var bpm = 100
    @Volatile var beatsPerBar = 4
    @Volatile var listener: BeatListener? = null

    private val main = Handler(Looper.getMainLooper())
    private var thread: Thread? = null
    @Volatile private var running = false

    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "plinky-metronome").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        thread?.join(800)
        thread = null
    }

    private fun synthClick(sr: Int, freq: Float, amp: Float, ms: Int): ShortArray {
        val len = sr * ms / 1000
        val out = ShortArray(len)
        for (i in 0 until len) {
            val t = i.toFloat() / sr
            val env = exp(-t * 90f)
            out[i] = (sin(2.0 * PI * freq * t) * env * amp * 32767).toInt().toShort()
        }
        return out
    }

    private fun loop() {
        val sr = 44100
        val click = synthClick(sr, 3000f, 0.5f, 40)
        val accent = synthClick(sr, 3800f, 0.85f, 40)

        val minBuf = AudioTrack.getMinBufferSize(
            sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sr)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuf * 2, 8192))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()

        val chunk = ShortArray(1024)
        var frame = 0L
        var nextBeat = 0.0
        var beat = 0
        var bar = 0
        var active: ShortArray? = null
        var activeIdx = 0

        while (running) {
            val framesPerBeat = sr * 60.0 / bpm
            for (i in chunk.indices) {
                val g = frame + i
                if (g >= nextBeat) {
                    active = if (beat == 0) accent else click
                    activeIdx = 0
                    val head = track.playbackHeadPosition.toLong()
                    val buffered = (g - head).coerceAtLeast(0)
                    val audible = System.nanoTime() + buffered * 1_000_000_000L / sr
                    val b = bar
                    val bt = beat
                    main.post { listener?.onBeat(b, bt, audible) }
                    beat++
                    if (beat >= beatsPerBar) {
                        beat = 0
                        bar++
                    }
                    nextBeat += framesPerBeat
                }
                var s: Short = 0
                val a = active
                if (a != null && activeIdx < a.size) {
                    s = a[activeIdx]
                    activeIdx++
                }
                chunk[i] = s
            }
            track.write(chunk, 0, chunk.size)
            frame += chunk.size
        }

        try { track.stop() } catch (_: Exception) {}
        track.release()
    }
}
