package im.flume.plinky.audio

object Dsp {

    /**
     * Autocorrelation pitch detector. Returns frequency in Hz, or -1 if no
     * confident pitch. Good enough for single plucked ukulele strings.
     */
    fun detectPitch(buf: FloatArray, sampleRate: Int): Float {
        val n = buf.size
        var mean = 0f
        for (v in buf) mean += v
        mean /= n

        var energy = 0f
        for (v in buf) {
            val d = v - mean
            energy += d * d
        }
        if (energy < 1e-4f) return -1f

        val minLag = sampleRate / 1000   // 1000 Hz ceiling
        val maxLag = sampleRate / 70     // 70 Hz floor
        if (maxLag + 1 >= n) return -1f

        val corr = FloatArray(maxLag + 2)
        for (lag in minLag..maxLag + 1) {
            var sum = 0f
            var i = 0
            val limit = n - lag
            while (i < limit) {
                sum += (buf[i] - mean) * (buf[i + lag] - mean)
                i++
            }
            corr[lag] = sum / energy
        }

        // First strong local maximum above threshold = fundamental.
        for (lag in minLag + 1..maxLag) {
            val c = corr[lag]
            if (c > 0.5f && c >= corr[lag - 1] && c >= corr[lag + 1]) {
                val c0 = corr[lag - 1]
                val c2 = corr[lag + 1]
                val denom = c0 - 2f * c + c2
                val shift = if (denom != 0f) 0.5f * (c0 - c2) / denom else 0f
                return sampleRate / (lag + shift)
            }
        }
        return -1f
    }
}
