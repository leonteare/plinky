package im.flume.plinky

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import im.flume.plinky.audio.AppAudio
import im.flume.plinky.ui.BeatHeroFragment
import im.flume.plinky.ui.ChordSprintFragment
import im.flume.plinky.ui.MetronomeFragment
import im.flume.plinky.ui.TunerFragment

class MainActivity : AppCompatActivity() {

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                AppAudio.mic.start(this)
            } else {
                Toast.makeText(this, R.string.mic_needed, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_metronome -> MetronomeFragment()
                R.id.nav_beat -> BeatHeroFragment()
                R.id.nav_sprint -> ChordSprintFragment()
                else -> TunerFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            nav.selectedItemId = R.id.nav_tuner
        }

        if (!AppAudio.mic.hasPermission(this)) {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        AppAudio.mic.start(this)
    }

    override fun onPause() {
        AppAudio.metro.stop()
        AppAudio.mic.stop()
        super.onPause()
    }
}
