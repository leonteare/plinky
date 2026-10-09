package im.flume.plinky

/** Ukulele chord shapes, frets listed in G C E A string order (0 = open). */
class Chord(val name: String, val frets: IntArray)

object Chords {
    val C = Chord("C", intArrayOf(0, 0, 0, 3))
    val Am = Chord("Am", intArrayOf(2, 0, 0, 0))
    val F = Chord("F", intArrayOf(2, 0, 1, 0))
    val G = Chord("G", intArrayOf(0, 2, 3, 2))
    val G7 = Chord("G7", intArrayOf(0, 2, 1, 2))
    val Em = Chord("Em", intArrayOf(0, 4, 3, 2))
    val Dm = Chord("Dm", intArrayOf(2, 2, 1, 0))
    val A = Chord("A", intArrayOf(2, 1, 0, 0))
    val D = Chord("D", intArrayOf(2, 2, 2, 0))

    class SprintPair(val a: Chord, val b: Chord) {
        val label: String get() = "${a.name} ↔ ${b.name}"
        val key: String get() = "sprint_${a.name}_${b.name}"
    }

    val PAIRS = listOf(
        SprintPair(C, Am),
        SprintPair(C, F),
        SprintPair(Am, F),
        SprintPair(C, G7),
        SprintPair(C, G),
        SprintPair(F, G),
        SprintPair(Am, Dm),
        SprintPair(G, Em)
    )
}
