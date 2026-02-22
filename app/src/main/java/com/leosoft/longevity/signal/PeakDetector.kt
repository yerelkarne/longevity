package com.leosoft.longevity.signal

object PeakDetector {
    fun detectPeaks(samples: List<Double>, minDistance: Int, threshold: Double): List<Int> {
        if (samples.size < 3) return emptyList()
        val peaks = mutableListOf<Int>()
        var last = -minDistance
        for (i in 1 until samples.lastIndex) {
            val candidate = samples[i] > samples[i - 1] && samples[i] >= samples[i + 1] && samples[i] > threshold
            if (candidate && i - last >= minDistance) {
                peaks += i
                last = i
            }
        }
        return peaks
    }
}
