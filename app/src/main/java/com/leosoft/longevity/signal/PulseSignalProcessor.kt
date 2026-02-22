package com.leosoft.longevity.signal

import com.leosoft.longevity.model.PulseResult
import com.leosoft.longevity.model.PulseStatus
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PulseSignalProcessor {

    fun process(signal: List<Double>, fs: Double): PulseResult {
        if (fs < 15.0) return PulseResult(0, 0, "", PulseStatus.LOW_FPS, 0..0)
        if (signal.size < (fs * 10).toInt()) return PulseResult(0, 0, "", PulseStatus.INSUFFICIENT_DATA, 0..0)

        val mean = signal.average()
        val variance = signal.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance).coerceAtLeast(1e-6)
        val min = signal.minOrNull() ?: mean
        val max = signal.maxOrNull() ?: mean
        val dynamicRange = max - min

        if (dynamicRange < 1.5) return PulseResult(0, 0, "", PulseStatus.NO_FINGER, 0..0)
        if (std < 0.8) return PulseResult(0, 0, "", PulseStatus.HOLD_STILL, 0..0)
        if (mean > 252 && dynamicRange < 3.0) return PulseResult(0, 0, "", PulseStatus.SATURATED, 0..0)

        val normalized = signal.map { (it - mean) / std }
        val filtered = Filters.bandPass(normalized, fs)

        val threshold = filtered.map { abs(it) }.average() * 0.2
        val minDistance = (fs * (60.0 / 180.0)).toInt().coerceAtLeast(3)
        val peaks = PeakDetector.detectPeaks(filtered, minDistance, threshold)
        if (peaks.size < 5) return PulseResult(0, 20, "low", PulseStatus.INSUFFICIENT_DATA, 0..0)

        val rr = peaks.zipWithNext { a, b -> (b - a).toDouble() }
        val median = rr.sorted()[rr.size / 2]
        val bpm = (60.0 * fs / median).roundToInt()

        val meanRr = rr.average()
        val rrStd = sqrt(rr.map { (it - meanRr) * (it - meanRr) }.average())
        val consistency = (1.0 - (rrStd / meanRr).coerceIn(0.0, 1.0))
        val snrLike = (filtered.map { abs(it) }.average() / (filtered.map { abs(it - filtered.average()) }.average().coerceAtLeast(1e-6))).coerceIn(0.0, 2.0) / 2.0
        val countScore = (peaks.size / 16.0).coerceIn(0.0, 1.0)
        val quality = ((consistency * 0.5 + snrLike * 0.3 + countScore * 0.2) * 100).roundToInt().coerceIn(0, 100)

        val windows = listOf(10, 15, 20).mapNotNull { sec ->
            val count = (sec * fs).toInt()
            if (filtered.size <= count) null else bpmFromWindow(filtered.takeLast(count), fs)
        }
        val unstable = windows.size >= 2 && (windows.maxOrNull()!! - windows.minOrNull()!! > 10)

        val status = when {
            unstable -> PulseStatus.UNSTABLE
            quality < 40 -> PulseStatus.LOW_QUALITY
            else -> PulseStatus.SUCCESS
        }
        val conf = (bpm - (100 - quality) / 8).coerceAtLeast(35)..(bpm + (100 - quality) / 8)
        val label = if (quality < 40) "low" else if (quality < 70) "medium" else "high"
        return PulseResult(bpm, quality, label, status, conf)
    }

    private fun bpmFromWindow(window: List<Double>, fs: Double): Int? {
        val peaks = PeakDetector.detectPeaks(window, (fs * (60.0 / 180.0)).toInt().coerceAtLeast(3), 0.1)
        if (peaks.size < 4) return null
        val rr = peaks.zipWithNext { a, b -> (b - a).toDouble() }
        return (60.0 * fs / rr.sorted()[rr.size / 2]).roundToInt()
    }
}
