package im.flume.plinky.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.flume.plinky.Chords
import im.flume.plinky.Prefs
import im.flume.plinky.R
import im.flume.plinky.audio.AppAudio
import im.flume.plinky.audio.MicEngine

class ChordSprintFragment : Fragment(R.layout.fragment_chord_sprint), MicEngine.Listener {

    private lateinit var spinner: Spinner
    private lateinit var chordView: ChordDiagramView
    private lateinit var countText: TextView
    private lateinit var timerText: TextView
    private lateinit var startBtn: MaterialButton
    private lateinit var bestText: TextView
    private lateinit var plinksText: TextView

    private var pair = Chords.PAIRS[0]
    private var playing = false
    private var count = 0
    private var showingA = true
    private var countdown: CountDownTimer? = null
    private var gameTimer: CountDownTimer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        spinner = view.findViewById(R.id.pairSpinner)
        chordView = view.findViewById(R.id.chordView)
        countText = view.findViewById(R.id.countText)
        timerText = view.findViewById(R.id.timerText)
        startBtn = view.findViewById(R.id.startBtn)
        bestText = view.findViewById(R.id.bestText)
        plinksText = view.findViewById(R.id.plinksText)

        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            Chords.PAIRS.map { it.label }
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                pair = Chords.PAIRS[pos]
                if (!playing) {
                    chordView.setChord(pair.a)
                    updateBestLabel()
                }
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        chordView.setChord(pair.a)
        startBtn.setOnClickListener { if (playing) cancelGame() else startGame() }
        updateBestLabel()
        updatePlinksLabel()
    }

    private fun updateBestLabel() {
        val best = Prefs.getInt(requireContext(), pair.key, -1)
        bestText.text = if (best < 0) getString(R.string.best_none) else "Best: $best changes"
    }

    private fun updatePlinksLabel() {
        plinksText.text = getString(
            R.string.plinks_lifetime,
            Prefs.getInt(requireContext(), "plinks", 0)
        )
    }

    private fun startGame() {
        if (!AppAudio.mic.start(requireContext())) {
            timerText.text = "!"
            bestText.text = getString(R.string.mic_needed)
            return
        }
        count = 0
        showingA = true
        countText.text = "0"
        chordView.setChord(pair.a)
        spinner.isEnabled = false
        startBtn.setText(R.string.stop)
        requireView().keepScreenOn = true

        countdown = object : CountDownTimer(3000, 1000) {
            override fun onTick(ms: Long) {
                timerText.text = ((ms / 1000) + 1).toString()
            }

            override fun onFinish() {
                playing = true
                timerText.text = "60.0"
                gameTimer = object : CountDownTimer(60_000, 100) {
                    override fun onTick(ms: Long) {
                        timerText.text = String.format("%.1f", ms / 1000f)
                    }

                    override fun onFinish() {
                        finishGame()
                    }
                }.start()
            }
        }.start()
    }

    private fun cancelGame() {
        countdown?.cancel()
        gameTimer?.cancel()
        playing = false
        spinner.isEnabled = true
        startBtn.setText(R.string.start)
        timerText.text = "60"
        view?.keepScreenOn = false
    }

    private fun finishGame() {
        playing = false
        spinner.isEnabled = true
        startBtn.setText(R.string.start)
        timerText.text = "0.0"
        view?.keepScreenOn = false

        val ctx = requireContext()
        val prevBest = Prefs.getInt(ctx, pair.key, -1)
        val isRecord = count > prevBest
        if (isRecord) Prefs.setInt(ctx, pair.key, count)
        Prefs.addToInt(ctx, "plinks", count)
        updateBestLabel()
        updatePlinksLabel()

        MaterialAlertDialogBuilder(ctx)
            .setTitle("$count changes!")
            .setMessage(
                "${pair.label} in 60 seconds." +
                    (if (isRecord) "\nNew personal best! 🎉" else "")
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onStrum(atNanos: Long, energy: Float) {
        if (!playing) return
        count++
        countText.text = count.toString()
        showingA = !showingA
        chordView.setChord(if (showingA) pair.a else pair.b)
    }

    override fun onStart() {
        super.onStart()
        AppAudio.mic.addListener(this)
        AppAudio.mic.start(requireContext())
    }

    override fun onStop() {
        cancelGame()
        AppAudio.mic.removeListener(this)
        super.onStop()
    }
}
