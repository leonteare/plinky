package im.flume.plinky.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import im.flume.plinky.R
import im.flume.plinky.audio.AppAudio
import im.flume.plinky.audio.MicEngine

class TunerFragment : Fragment(R.layout.fragment_tuner), MicEngine.Listener {

    private lateinit var tunerView: TunerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tunerView = view.findViewById(R.id.tunerView)
    }

    override fun onStart() {
        super.onStart()
        AppAudio.mic.pitchEnabled = true
        AppAudio.mic.addListener(this)
        AppAudio.mic.start(requireContext())
    }

    override fun onStop() {
        AppAudio.mic.pitchEnabled = false
        AppAudio.mic.removeListener(this)
        super.onStop()
    }

    override fun onPitch(freqHz: Float) {
        tunerView.update(freqHz)
    }
}
