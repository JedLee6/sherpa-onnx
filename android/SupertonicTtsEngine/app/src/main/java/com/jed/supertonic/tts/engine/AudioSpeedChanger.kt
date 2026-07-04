package com.jed.supertonic.tts.engine

class AudioSpeedChanger(val sampleRate: Int, val speed: Float) {
    private val sonic: Sonic? = if (speed != 1.0f) Sonic(sampleRate, 1).apply {
        this.speed = speed
        this.pitch = 1.0f
        this.rate = 1.0f
    } else null

    fun process(inputSamples: FloatArray): FloatArray {
        if (sonic == null || inputSamples.isEmpty()) return inputSamples

        // Convert FloatArray to short array
        val shortArray = ShortArray(inputSamples.size)
        for (i in inputSamples.indices) {
            var value = (inputSamples[i] * 32767.0f).toInt()
            if (value > Short.MAX_VALUE) value = Short.MAX_VALUE.toInt()
            if (value < Short.MIN_VALUE) value = Short.MIN_VALUE.toInt()
            shortArray[i] = value.toShort()
        }

        sonic.writeShortToStream(shortArray, shortArray.size)
        
        val availableSamples = sonic.samplesAvailable()
        if (availableSamples <= 0) return FloatArray(0)
        
        val outShortArray = ShortArray(availableSamples)
        val readSamples = sonic.readShortFromStream(outShortArray, availableSamples)
        
        val outFloatArray = FloatArray(readSamples)
        for (i in 0 until readSamples) {
            outFloatArray[i] = outShortArray[i].toFloat() / 32768.0f
        }
        return outFloatArray
    }
    
    fun flush(): FloatArray {
        if (sonic == null) return FloatArray(0)
        sonic.flushStream()
        
        val availableSamples = sonic.samplesAvailable()
        if (availableSamples <= 0) return FloatArray(0)
        
        val outShortArray = ShortArray(availableSamples)
        val readSamples = sonic.readShortFromStream(outShortArray, availableSamples)
        
        val outFloatArray = FloatArray(readSamples)
        for (i in 0 until readSamples) {
            outFloatArray[i] = outShortArray[i].toFloat() / 32768.0f
        }
        return outFloatArray
    }

    companion object {
        fun oneShotProcess(inputSamples: FloatArray, sampleRate: Int, speed: Float): FloatArray {
            if (speed == 1.0f || inputSamples.isEmpty()) return inputSamples
            val changer = AudioSpeedChanger(sampleRate, speed)
            val p1 = changer.process(inputSamples)
            val p2 = changer.flush()
            val combined = FloatArray(p1.size + p2.size)
            p1.copyInto(combined, 0)
            p2.copyInto(combined, p1.size)
            return combined
        }
    }
}
