package im.flume.plinky.audio

/** Shared audio engines for the whole app. */
object AppAudio {
    val mic = MicEngine()
    val metro = Metronome()
}
