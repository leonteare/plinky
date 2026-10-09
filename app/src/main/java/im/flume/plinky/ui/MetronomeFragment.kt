package im.flume.plinky.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import im.flume.plinky.R
import im.flume.plinky.audio.AppAudio
import im.flume.plinky.audio.Metronome

class MetronomeFragment : Fragment(R.layout.fragment_metronome), Metronome.BeatListener {

    private lateinit var tempoText: TextView
    private lateinit var dots: BeatDotsView
    private lateinit var startBtn: MaterialButton

    private val metro = AppAudio.metro

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tempoText = view.findViewById(R.id.tempoText)
        dots = view.findViewById(R.id.dots)
        startBtn = view.findViewById(R.id.startBtn)
        val slider = view.findViewById<Slider>(R.id.bpmSlider)
        val sigToggle = view.findViewById<MaterialButtonToggleGroup>(R.id.sigToggle)

        slider.value = metro.bpm.toFloat().coerceIn(40f, 208f)
        tempoText.text = metro.bpm.toString()
        dots.configure(metro.beatsPerBar)

        slider.addOnChangeListener { _, value, _ ->
            metro.bpm = value.toInt()
            tempoText.text = metro.bpm.toString()
        }

        sigToggle.check(if (metro.beatsPerBar == 3) R.id.sig34 else R.id.sig44)
        sigToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            metro.beatsPerBar = if (checkedId == R.id.sig34) 3 else 4
            dots.configure(metro.beatsPerBar)
        }

        startBtn.setOnClickListener {
            if (metro.isRunning) {
                metro.stop()
                dots.reset()
                startBtn.setText(R.string.start)
            } else {
                metro.listener = this
                metro.start()
                startBtn.setText(R.string.stop)
            }
        }
    }

    override fun onBeat(bar: Int, beat: Int, audibleAtNanos: Long) {
        if (!isAdded) return
        dots.pulse(beat)
    }

    override fun onStart() {
        super.onStart()
        metro.listener = this
        startBtn.setText(if (metro.isRunning) R.string.stop else R.string.start)
    }

    override fun onStop() {
        metro.stop()
        metro.listener = null
        super.onStop()
    }
}
