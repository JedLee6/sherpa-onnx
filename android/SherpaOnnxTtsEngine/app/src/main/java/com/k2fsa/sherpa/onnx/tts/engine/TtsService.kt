package com.k2fsa.sherpa.onnx.tts.engine

import android.media.AudioFormat
import com.k2fsa.sherpa.onnx.GenerationConfig
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.android.gms.tasks.Tasks

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
    private val languageIdentifier by lazy {
        LanguageIdentification.getClient()
    }

    override fun onCreate() {
        Log.i(TAG, "onCreate tts service")
        super.onCreate()

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
        super.onDestroy()
    }

    // https://developer.android.com/reference/kotlin/android/speech/tts/TextToSpeechService#onislanguageavailable
    override fun onIsLanguageAvailable(_lang: String?, _country: String?, _variant: String?): Int {
        val lang = _lang ?: ""

        if (lang == TtsEngine.lang || lang == TtsEngine.lang2) {
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

        return if (lang == TtsEngine.lang || lang == TtsEngine.lang2) {
            Log.i(TAG, "creating tts, lang :$lang")
            TtsEngine.createTts(application)
            TextToSpeech.LANG_AVAILABLE
        } else {
            Log.i(TAG, "lang $lang not supported, tts engine lang: ${TtsEngine.lang}, ${TtsEngine.lang2}")
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onStop() {}

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) {
            return
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
        val detected = try {
            Tasks.await(languageIdentifier.identifyLanguage(text))
        } catch (e: Exception) {
            Log.e(TAG, "Language identification failed in TtsService", e)
            "und"
        }
        val iso1 = Languages.mapGoogleMlKitToIso1(detected) ?: TtsEngine.supertonicLang
        val isChinese = (iso1 == "zh")

        val selectedTts = if (isChinese && TtsEngine.matchaTts != null) {
            TtsEngine.matchaTts!!
        } else {
            TtsEngine.supertonicTts ?: TtsEngine.tts!!
        }

        Log.i(TAG, "text: $text, engineSpeed: $engineSpeed, isChinese: $isChinese")

        // Note that AudioFormat.ENCODING_PCM_FLOAT requires API level >= 24
        // callback.start(selectedTts.sampleRate(), AudioFormat.ENCODING_PCM_FLOAT, 1)

        callback.start(selectedTts.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isBlank() || text.isEmpty()) {
            callback.done()
            return
        }

        val ttsCallback: (FloatArray) -> Int = fun(floatSamples): Int {
            // convert FloatArray to ByteArray
            val samples = floatArrayToByteArray(floatSamples)
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

        Log.i(TAG, "text: $text")
        val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = engineSpeed)
        if (selectedTts == TtsEngine.supertonicTts || selectedTts != TtsEngine.matchaTts) {
            genConfig.extra = mapOf("lang" to iso1)
        }

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
