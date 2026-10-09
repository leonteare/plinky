package im.flume.plinky.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import im.flume.plinky.Prefs
import im.flume.plinky.R
import im.flume.plinky.audio.AppAudio
import im.flume.plinky.audio.Metronome
import im.flume.plinky.audio.MicEngine
import kotlin.math.abs
import kotlin.random.Random

class BeatHeroFragment : Fragment(R.layout.fragment_beat_hero),
    MicEngine.Listener, Metronome.BeatListener {

    private lateinit var lane: BeatLaneView
    private lateinit var feedbackText: TextView
    private lateinit var scoreText: TextView
    private lateinit var comboText: TextView
    private lateinit var bestText: TextView
    private lateinit var startBtn: MaterialButton
    private lateinit var sigToggle: MaterialButtonToggleGroup
    private lateinit var levelChips: ChipGroup
    private lateinit var bpmSlider: Slider
    private lateinit var bpmLabel: TextView
    private lateinit var offsetSlider: Slider

    private val metro = AppAudio.metro
    private val handler = Handler(Looper.getMainLooper())

    private val totalBars = 8

    private class Expected(val atNanos: Long, val beat: Int) {
        var consumed = false
        var missed = false
    }

    private var playing = false
    private var currentBar = -1
    private var score = 0
    private var combo = 0
    private var highlightTotal = 0
    private var patterns: Array<BooleanArray> = arrayOf()
    private val expected = mutableListOf<Expected>()

    private val beats: Int
        get() = if (sigToggle.checkedButtonId == R.id.sig34) 3 else 4

    private val level: Int
        get() = when (levelChips.checkedChipId) {
            R.id.level2 -> 2
            R.id.level3 -> 3
            R.id.level4 -> 4
            else -> 1
        }

    private val bestKey: String get() = "bh_best_${beats}_$level"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lane = view.findViewById(R.id.lane)
        feedbackText = view.findViewById(R.id.feedbackText)
        scoreText = view.findViewById(R.id.scoreText)
        comboText = view.findViewById(R.id.comboText)
        bestText = view.findViewById(R.id.bestText)
        startBtn = view.findViewById(R.id.startBtn)
        sigToggle = view.findViewById(R.id.sigToggle)
        levelChips = view.findViewById(R.id.levelChips)
        bpmSlider = view.findViewById(R.id.bpmSlider)
        bpmLabel = view.findViewById(R.id.bpmLabel)
        offsetSlider = view.findViewById(R.id.offsetSlider)

        lane.configure(beats)

        bpmSlider.addOnChangeListener { _, value, _ ->
            bpmLabel.text = "${value.toInt()} BPM"
        }
        bpmLabel.text = "${bpmSlider.value.toInt()} BPM"

        offsetSlider.value = Prefs.getInt(requireContext(), "cal_offset", 50)
            .toFloat().coerceIn(-200f, 200f)
        offsetSlider.addOnChangeListener { _, value, _ ->
            Prefs.setInt(requireContext(), "cal_offset", value.toInt())
        }

        sigToggle.addOnButtonCheckedListener { _, _, isChecked ->
            if (isChecked && !playing) {
                lane.configure(beats)
                updateBestLabel()
            }
        }
        levelChips.setOnCheckedStateChangeListener { _, _ ->
            if (!playing) updateBestLabel()
        }

        startBtn.setOnClickListener {
            if (playing) stopGame() else startGame()
        }

        updateBestLabel()
        updateScoreUi()
    }

    private fun updateBestLabel() {
        val best = Prefs.getInt(requireContext(), bestKey, -1)
        bestText.text = if (best < 0) getString(R.string.best_none) else "Best $best%"
    }

    private fun updateScoreUi() {
        scoreText.text = "Score $score"
        comboText.text = "Combo $combo"
    }

    private fun makePattern(beatCount: Int, lvl: Int): BooleanArray = when (lvl) {
        1 -> BooleanArray(beatCount) { it == 0 }
        2 -> BooleanArray(beatCount) { it == 0 || it == 2 }
        3 -> BooleanArray(beatCount) { true }
        else -> {
            val p = BooleanArray(beatCount) { Random.nextBoolean() }
            if (p.none { it }) p[Random.nextInt(beatCount)] = true
            p
        }
    }

    private fun startGame() {
        if (!AppAudio.mic.start(requireContext())) {
            feedbackText.text = getString(R.string.mic_needed)
            return
        }
        val b = beats
        val lvl = level
        patterns = Array(totalBars + 1) { bar ->
            if (bar == 0) BooleanArray(b) else makePattern(b, lvl)
        }
        highlightTotal = patterns.drop(1).sumOf { p -> p.count { it } }
        score = 0
        combo = 0
        currentBar = -1
        expected.clear()
        playing = true

        lane.configure(b)
        updateScoreUi()
        setControlsEnabled(false)
        startBtn.setText(R.string.stop)
        requireView().keepScreenOn = true

        metro.stop()
        metro.bpm = bpmSlider.value.toInt()
        metro.beatsPerBar = b
        metro.listener = this
        metro.start()
    }

    private fun stopGame() {
        playing = false
        metro.stop()
        handler.removeCallbacksAndMessages(null)
        lane.reset()
        lane.configure(beats)
        setControlsEnabled(true)
        startBtn.setText(R.string.start)
        feedbackText.text = getString(R.string.beat_hint)
        view?.keepScreenOn = false
    }

    private fun setControlsEnabled(enabled: Boolean) {
        sigToggle.isEnabled = enabled
        for (i in 0 until sigToggle.childCount) sigToggle.getChildAt(i).isEnabled = enabled
        for (i in 0 until levelChips.childCount) levelChips.getChildAt(i).isEnabled = enabled
        bpmSlider.isEnabled = enabled
        offsetSlider.isEnabled = enabled
    }

    override fun onBeat(bar: Int, beat: Int, audibleAtNanos: Long) {
        if (!playing || !isAdded) return
        currentBar = bar

        if (bar == 0) {
            feedbackText.text = (metro.beatsPerBar - beat).toString()
            lane.setCurrent(beat)
            return
        }
        if (bar > totalBars) {
            finishGame()
            return
        }
        if (beat == 0) {
            lane.setPattern(patterns[bar])
            feedbackText.text = "Bar $bar / $totalBars"
        }
        lane.setCurrent(beat)

        if (patterns[bar][beat]) {
            val exp = Expected(audibleAtNanos, beat)
            expected.add(exp)
            val delayMs = ((audibleAtNanos - System.nanoTime()) / 1_000_000L + 300L)
                .coerceAtLeast(0L)
            handler.postDelayed({
                if (playing && !exp.consumed && !exp.missed) {
                    exp.missed = true
                    combo = 0
                    lane.flash(exp.beat, lane.colorMiss)
                    feedbackText.text = "Miss"
                    updateScoreUi()
                }
            }, delayMs)
        }
    }

    override fun onStrum(atNanos: Long, energy: Float) {
        if (!playing || currentBar < 1) return
        val offsetNs = offsetSlider.value.toLong() * 1_000_000L
        val t = atNanos - offsetNs

        var best: Expected? = null
        var bestDelta = Long.MAX_VALUE
        for (e in expected) {
            if (e.consumed || e.missed) continue
            val d = abs(t - e.atNanos)
            if (d < bestDelta) {
                bestDelta = d
                best = e
            }
        }

        if (best != null && bestDelta <= 280_000_000L) {
            best.consumed = true
            val deltaMs = bestDelta / 1_000_000L
            if (deltaMs <= 110) {
                score += 100
                combo++
                lane.flash(best.beat, lane.colorPerfect)
                feedbackText.text = "Perfect!"
            } else {
                score += 50
                combo++
                lane.flash(best.beat, lane.colorGood)
                feedbackText.text = "Good"
            }
        } else {
            combo = 0
            feedbackText.text = "Off-beat"
        }
        updateScoreUi()
    }

    private fun finishGame() {
        val finalScore = score
        val maxScore = highlightTotal * 100
        stopGame()

        val pct = if (maxScore > 0) finalScore * 100 / maxScore else 0
        val stars = when {
            pct >= 90 -> "★★★"
            pct >= 70 -> "★★☆"
            pct >= 40 -> "★☆☆"
            else -> "☆☆☆"
        }
        val prevBest = Prefs.getInt(requireContext(), bestKey, -1)
        val isRecord = pct > prevBest
        if (isRecord) Prefs.setInt(requireContext(), bestKey, pct)
        updateBestLabel()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("$stars  $pct%")
            .setMessage(
                "Score $finalScore of $maxScore" +
                    (if (isRecord) "\nNew personal best! 🎉" else "")
            )
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        AppAudio.mic.addListener(this)
        metro.listener = this
    }

    override fun onStop() {
        if (playing) stopGame()
        metro.stop()
        metro.listener = null
        AppAudio.mic.removeListener(this)
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }
}
