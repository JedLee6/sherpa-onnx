package com.k2fsa.sherpa.onnx.tts.engine

import PreferenceHelper
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

const val MIN_TTS_SPEED = 0.1f
const val MAX_TTS_SPEED = 5.0f

object TtsEngine {
    var tts: OfflineTts? = null

    // https://en.wikipedia.org/wiki/ISO_639-3
    // Example:
    // eng for English,
    // deu for German
    // cmn for Mandarin
    var lang: String? = null

    // if a model supports two languages, set also lang2
    var lang2: String? = null

    // for Supertonic TTS: language code in ISO 639-1 format, e.g., "en", "zh", "ja"
    val supertonicLangState: MutableState<String> = mutableStateOf("en")

    var supertonicLang: String
        get() = supertonicLangState.value
        set(value) {
            supertonicLangState.value = value
        }


    val speedState: MutableState<Float> = mutableFloatStateOf(1.0F)
    val speakerIdState: MutableState<Int> = mutableIntStateOf(0)

    var speed: Float
        get() = speedState.value
        set(value) {
            speedState.value = value
        }

    var speakerId: Int
        get() = speakerIdState.value
        set(value) {
            speakerIdState.value = value
        }

    val modelState: MutableState<ModelConfig> = mutableStateOf(Models.supportedModels[0])

    var currentModel: ModelConfig
        get() = modelState.value
        set(value) {
            modelState.value = value
        }

    private var modelDir: String? = null
    private var modelName: String? = null
    private var acousticModelName: String? = null // for matcha tts
    private var vocoder: String? = null // for matcha tts
    private var voices: String? = null // for kokoro
    private var ruleFsts: String? = null
    private var ruleFars: String? = null
    private var lexicon: String? = null
    private var dataDir: String? = null
    private var assets: AssetManager? = null
    private var isKitten = false
    var isSupertonic = false
    private var durationPredictor: String? = null
    private var textEncoder: String? = null
    private var vectorEstimator: String? = null
    private var supertonicVocoder: String? = null
    private var ttsJson: String? = null
    private var unicodeIndexer: String? = null
    private var voiceStyle: String? = null

    init {
        // Models are now dynamically loaded via PreferenceHelper and Models object
    }

    fun createTts(context: Context) {
        Log.i(TAG, "Init Next-gen Kaldi TTS")
        if (tts == null) {
            initTts(context)
        }
    }

    fun updateTts(context: Context) {
        initTts(context)
    }

    private fun initTts(context: Context) {
        val preferenceHelper = PreferenceHelper(context)
        val modelId = preferenceHelper.getModel()

        try {
            realInitTts(context, modelId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model $modelId: $e")
            if (modelId != "supertonic-3-tts") {
                Log.i(TAG, "Falling back to default model supertonic-3-tts")
                preferenceHelper.setModel("supertonic-3-tts")
                try {
                    realInitTts(context, "supertonic-3-tts")
                } catch (e2: Exception) {
                    Log.e(TAG, "Even default model failed: $e2")
                    tts = null
                }
            } else {
                tts = null
            }
        }

        // Ensure lang is never null after init, so TtsService doesn't crash
        if (lang == null) {
            lang = "eng"
        }
    }

    private fun realInitTts(context: Context, modelId: String) {
        assets = context.assets

        val preferenceHelper = PreferenceHelper(context)
        val config = Models.getModel(modelId)

        // Pre-validate: check all required files exist in assets BEFORE native init.
        // Native crashes (SIGSEGV) from missing files cannot be caught by try-catch.
        if (!validateModelAssets(context, config)) {
            throw IllegalStateException(
                "Model '${config.name}' is missing required asset files. " +
                "Cannot initialize safely."
            )
        }

        currentModel = config

        modelDir = config.modelDir
        modelName = config.modelName
        acousticModelName = config.acousticModelName
        vocoder = config.vocoder
        voices = config.voices
        lexicon = config.lexicon
        dataDir = config.dataDir
        lang = config.lang
        isSupertonic = config.isSupertonic
        durationPredictor = config.durationPredictor
        textEncoder = config.textEncoder
        vectorEstimator = config.vectorEstimator
        supertonicVocoder = config.supertonicVocoder
        ttsJson = config.ttsJson
        unicodeIndexer = config.unicodeIndexer
        voiceStyle = config.voiceStyle

        var currentDataDir = dataDir
        if (currentDataDir != null && currentDataDir.isNotEmpty()) {
            val newDir = copyDataDir(context, currentDataDir)
            currentDataDir = "$newDir/$currentDataDir"
        }

        var currentDictDir = config.dictDir
        if (currentDictDir.isNotEmpty()) {
            val newDir = copyDataDir(context, currentDictDir)
            currentDictDir = "$newDir/$currentDictDir"
        }

        val ttsConfig = getOfflineTtsConfig(
            modelDir = modelDir!!,
            modelName = modelName ?: "",
            acousticModelName = acousticModelName ?: "",
            vocoder = vocoder ?: "",
            voices = voices ?: "",
            lexicon = lexicon ?: "",
            dataDir = currentDataDir ?: "",
            dictDir = currentDictDir,
            ruleFsts = ruleFsts ?: "",
            ruleFars = ruleFars ?: "",
            isKitten = isKitten,
            isSupertonic = isSupertonic,
            durationPredictor = durationPredictor ?: "",
            textEncoder = textEncoder ?: "",
            vectorEstimator = vectorEstimator ?: "",
            supertonicVocoder = supertonicVocoder ?: "",
            ttsJson = ttsJson ?: "",
            unicodeIndexer = unicodeIndexer ?: "",
            voiceStyle = voiceStyle ?: "",
        )

        speed = preferenceHelper.getSpeed()
        speakerId = preferenceHelper.getSid()

        if (isSupertonic) {
            supertonicLang = preferenceHelper.getLanguage(config.supertonicLang)
        }

        tts = OfflineTts(assetManager = assets, config = ttsConfig)
    }

    /**
     * Pre-validate that all required asset files for a model exist.
     * This prevents native crashes (SIGSEGV) that cannot be caught by try-catch.
     */
    fun validateModelAssets(context: Context, config: ModelConfig): Boolean {
        if (config.requiredFiles.isEmpty()) {
            Log.w(TAG, "Model '${config.name}' has no requiredFiles list, skipping validation")
            return true
        }
        for (file in config.requiredFiles) {
            try {
                context.assets.open(file).close()
            } catch (e: IOException) {
                Log.e(TAG, "Missing required asset file: $file for model '${config.name}'")
                return false
            }
        }
        Log.i(TAG, "All ${config.requiredFiles.size} required files validated for model '${config.name}'")
        return true
    }


    private fun copyDataDir(context: Context, dataDir: String): String {
        Log.i(TAG, "data dir is $dataDir")
        copyAssets(context, dataDir)

        val newDataDir = context.getExternalFilesDir(null)!!.absolutePath
        Log.i(TAG, "newDataDir: $newDataDir")
        return newDataDir
    }

    private fun copyAssets(context: Context, path: String) {
        val assets: Array<String>?
        try {
            assets = context.assets.list(path)
            if (assets!!.isEmpty()) {
                copyFile(context, path)
            } else {
                val fullPath = "${context.getExternalFilesDir(null)}/$path"
                val dir = File(fullPath)
                dir.mkdirs()
                for (asset in assets.iterator()) {
                    val p: String = if (path == "") "" else "$path/"
                    copyAssets(context, p + asset)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy $path. $ex")
        }
    }

    private fun copyFile(context: Context, filename: String) {
        try {
            val istream = context.assets.open(filename)
            val newFilename = context.getExternalFilesDir(null).toString() + "/" + filename
            val ostream = FileOutputStream(newFilename)
            // Log.i(TAG, "Copying $filename to $newFilename")
            val buffer = ByteArray(1024)
            var read = 0
            while (read != -1) {
                ostream.write(buffer, 0, read)
                read = istream.read(buffer)
            }
            istream.close()
            ostream.flush()
            ostream.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to copy $filename, $ex")
        }
    }
}
