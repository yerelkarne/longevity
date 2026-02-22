package com.leosoft.longevity.signal

import com.leosoft.longevity.model.PulseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PulseSignalProcessorTest {
    private val processor = PulseSignalProcessor()

    @Test
    fun `returns bpm for clean sinusoidal signal`() {
        val fs = 30.0
        val freqHz = 1.2 // 72 bpm
        val signal = (0 until 900).map { i -> 120.0 + 20.0 * sin(2 * PI * freqHz * i / fs) }
        val result = processor.process(signal, fs)
        assertTrue(result.bpm in 68..76)
        assertTrue(result.quality >= 40)
    }

    @Test
    fun `low fps returns low fps status`() {
        val signal = List(300) { 100.0 }
        val result = processor.process(signal, 10.0)
        assertEquals(PulseStatus.LOW_FPS, result.status)
    }
}
