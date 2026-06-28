package com.k2fsa.sherpa.onnx.tts.engine

import android.media.AudioFormat
import com.k2fsa.sherpa.onnx.GenerationConfig
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.languagedetector.LanguageDetector
import com.google.mediapipe.tasks.text.languagedetector.LanguageDetector.LanguageDetectorOptions

/*
https://developer.android.com/reference/java/util/Locale#getISO3Language()
https://developer.android.com/reference/java/util/Locale#getISO3Country()

eng, USA,
eng, USA, POSIX
eng,
eng, GBR
afr,
afr, NAM
afr, ZAF
agq
agq, CMR
aka,
aka, GHA
amh,
amh, ETH
ara,
ara, 001
ara, ARE
ara, BHR,
deu
deu, AUT
deu, BEL
deu, CHE
deu, ITA
deu, ITA
deu, LIE
deu, LUX
spa,
spa, 419
spa, ARG,
spa, BRA
fra,
fra, BEL,
fra, FRA,

E  Failed to check TTS data, no activity found for Intent
{ act=android.speech.tts.engine.CHECK_TTS_DATA pkg=com.k2fsa.sherpa.chapter5 })

E Failed to get default language from engine com.k2fsa.sherpa.chapter5
Engine failed voice data integrity check (null return)com.k2fsa.sherpa.chapter5
Failed to get default language from engine com.k2fsa.sherpa.chapter5

*/

class TtsService : TextToSpeechService() {
    private var languageDetector: LanguageDetector? = null

    override fun onCreate() {
        Log.i(TAG, "onCreate tts service")
        super.onCreate()

        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("language_detector.tflite")
                .build()
            val options = LanguageDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            languageDetector = LanguageDetector.createFromOptions(this, options)
            Log.i(TAG, "MediaPipe Language Detector initialized successfully in TtsService")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe Language Detector in TtsService", e)
        }

        // see https://github.com/Miserlou/Android-SDK-Samples/blob/master/TtsEngine/src/com/example/android/ttsengine/RobotSpeakTtsService.java#L68
        val currentLang = TtsEngine.lang
        if (currentLang != null) {
            onLoadLanguage(currentLang, "", "")
        } else {
            Log.w(TAG, "TtsEngine.lang is null during TtsService.onCreate, skipping onLoadLanguage")
            TtsEngine.createTts(application)
        }
        if (TtsEngine.lang2 != null) {
            onLoadLanguage(TtsEngine.lang2, "", "")
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy tts service")
        languageDetector?.close()
        super.onDestroy()
    }

    // https://developer.android.com/reference/kotlin/android/speech/tts/TextToSpeechService#onislanguageavailable
    override fun onIsLanguageAvailable(_lang: String?, _country: String?, _variant: String?): Int {
        val lang = _lang ?: ""

        val iso1 = when (lang) {
            "eng" -> "en"
            "kor" -> "ko"
            "jpn" -> "ja"
            "zho", "cmn", "chi" -> "zh"
            else -> {
                Languages.supportedLanguages.find { Languages.getIso3Code(it.code) == lang }?.code
            }
        }

        if (iso1 != null || lang == TtsEngine.lang || lang == TtsEngine.lang2) {
            return TextToSpeech.LANG_AVAILABLE
        }

        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf(TtsEngine.lang ?: "eng", "", "")
    }

    // https://developer.android.com/reference/kotlin/android/speech/tts/TextToSpeechService#onLoadLanguage(kotlin.String,%20kotlin.String,%20kotlin.String)
    override fun onLoadLanguage(_lang: String?, _country: String?, _variant: String?): Int {
        Log.i(TAG, "onLoadLanguage: $_lang, $_country")
        val lang = _lang ?: ""

        val isAvailable = onIsLanguageAvailable(lang, _country, _variant)
        return if (isAvailable == TextToSpeech.LANG_AVAILABLE) {
            Log.i(TAG, "creating tts, lang :$lang")
            TtsEngine.createTts(application)
            TextToSpeech.LANG_AVAILABLE
        } else {
            Log.i(TAG, "lang $lang not supported")
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onStop() {}

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) {
            return
        }
        if (TtsEngine.tts == null) {
            Log.w(TAG, "TtsEngine.tts is null in onSynthesizeText, calling createTts")
            TtsEngine.createTts(application)
            if (TtsEngine.tts == null) {
                Log.e(TAG, "TtsEngine.tts is still null, cannot synthesize")
                callback.error()
                return
            }
        }
        val language = request.language
        val country = request.country
        val variant = request.variant
        val text = request.charSequenceText.toString()
        // Map Android TTS speech rate (where 100 == normal) to engine speed (1.0 == normal)
        // Allow per-request override from external apps; fallback to engine default if absent.
        val rate = runCatching { request.speechRate }.getOrDefault(-1)
        val engineSpeed = if (rate > 0) {
            // Map 100 -> 1.0f
            val mapped = rate / 100.0f
            mapped.coerceIn(MIN_TTS_SPEED, MAX_TTS_SPEED)
        } else {
            // Fallback to current engine/global setting
            TtsEngine.speed
        }

        val ret = onIsLanguageAvailable(language, country, variant)
        if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }
        val cjkLang = Languages.detectCjkLanguage(text)
        val iso1 = if (cjkLang != null) {
            Log.i(TAG, "Text: '$text', CJK language detected directly in TtsService: $cjkLang")
            cjkLang
        } else {
            val detected = try {
                val result = languageDetector?.detect(text)
                val prediction = result?.languagesAndScores()?.firstOrNull()
                prediction?.languageCode() ?: "und"
            } catch (e: Exception) {
                Log.e(TAG, "Language identification failed in TtsService", e)
                "und"
            }
            Languages.mapGoogleMlKitToIso1(detected) ?: TtsEngine.supertonicLang
        }
        val isChinese = (iso1 == "zh")

        val selectedTts = TtsEngine.supertonicTts ?: TtsEngine.tts!!

        val nativeRate = TtsEngine.tts!!.sampleRate()
        val generatorRate = selectedTts.sampleRate()
        val factor = generatorRate.toFloat() / nativeRate

        Log.i(TAG, "text: $text, engineSpeed: $engineSpeed, isChinese: $isChinese, nativeRate: $nativeRate, generatorRate: $generatorRate, factor: $factor")

        callback.start(nativeRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isBlank() || text.isEmpty()) {
            callback.done()
            return
        }

        val resampler = if (factor != 1.0f) {
            RealtimeResampler(factor)
        } else {
            null
        }

        val ttsCallback: (FloatArray) -> Int = fun(floatSamples): Int {
            val processed = resampler?.process(floatSamples) ?: floatSamples
            // convert FloatArray to ByteArray
            val samples = floatArrayToByteArray(processed)
            val maxBufferSize: Int = callback.maxBufferSize
            var offset = 0
            while (offset < samples.size) {
                val bytesToWrite = Math.min(maxBufferSize, samples.size - offset)
                callback.audioAvailable(samples, offset, bytesToWrite)
                offset += bytesToWrite
            }

            // 1 means to continue
            // 0 means to stop
            return 1
        }

        val targetSpeed = engineSpeed
        Log.i(TAG, "TtsService debug - text: '$text', selectedTts: $selectedTts, nativeRate: $nativeRate, generatorRate: $generatorRate, factor: $factor, resampler: $resampler, engineSpeed: $engineSpeed, targetSpeed: $targetSpeed")
        val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = targetSpeed)
        genConfig.extra = mapOf("lang" to iso1)

        selectedTts.generateWithConfigAndCallback(
            text = text,
            config = genConfig,
            callback = ttsCallback,
        )

        callback.done()
    }

    private fun floatArrayToByteArray(audio: FloatArray): ByteArray {
        // byteArray is actually a ShortArray
        val byteArray = ByteArray(audio.size * 2)
        for (i in audio.indices) {
            val sample = (audio[i] * 32767).toInt()
            byteArray[2 * i] = sample.toByte()
            byteArray[2 * i + 1] = (sample shr 8).toByte()
        }
        return byteArray
    }
}
