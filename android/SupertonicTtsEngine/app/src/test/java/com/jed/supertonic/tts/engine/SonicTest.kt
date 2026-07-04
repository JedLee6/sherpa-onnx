package com.jed.supertonic.tts.engine
import org.junit.Test

class SonicTest {
    @Test
    fun testSpeed() {
        val sampleRate = 16000
        val numSamples = sampleRate * 2 // 2 seconds of audio
        val input = FloatArray(numSamples) { Math.sin(it.toDouble() * 440.0 * 2.0 * Math.PI / sampleRate).toFloat() * 0.5f }
        
        val changer = AudioSpeedChanger(sampleRate, 2.0f)
        val p1 = changer.process(input)
        val p2 = changer.flush()
        
        val outSize = p1.size + p2.size
        println("Input size: ${input.size}, Output size: $outSize")
    }
}
