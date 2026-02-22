package com.leosoft.longevity.signal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Filters {
    fun bandPass(input: List<Double>, fs: Double, lowHz: Double = 0.7, highHz: Double = 3.0): List<Double> {
        if (input.isEmpty() || fs <= 0.0) return input
        val hp = biquadHighPass(input, fs, lowHz)
        return biquadLowPass(hp, fs, highHz)
    }

    private fun biquadLowPass(x: List<Double>, fs: Double, cutoff: Double): List<Double> {
        val q = 0.707
        val w0 = 2.0 * PI * cutoff / fs
        val alpha = sin(w0) / (2.0 * q)
        val c = cos(w0)
        val b0 = (1 - c) / 2
        val b1 = 1 - c
        val b2 = (1 - c) / 2
        val a0 = 1 + alpha
        val a1 = -2 * c
        val a2 = 1 - alpha
        return biquad(x, b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private fun biquadHighPass(x: List<Double>, fs: Double, cutoff: Double): List<Double> {
        val q = 0.707
        val w0 = 2.0 * PI * cutoff / fs
        val alpha = sin(w0) / (2.0 * q)
        val c = cos(w0)
        val b0 = (1 + c) / 2
        val b1 = -(1 + c)
        val b2 = (1 + c) / 2
        val a0 = 1 + alpha
        val a1 = -2 * c
        val a2 = 1 - alpha
        return biquad(x, b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private fun biquad(x: List<Double>, b0: Double, b1: Double, b2: Double, a1: Double, a2: Double): List<Double> {
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        return x.map { n ->
            val y = b0 * n + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = n
            y2 = y1
            y1 = y
            y
        }
    }
}
