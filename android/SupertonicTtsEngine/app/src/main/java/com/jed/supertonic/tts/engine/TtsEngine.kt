package com.jed.supertonic.tts.engine

import PreferenceHelper
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import android.widget.Toast
import com.jed.supertonic.tts.engine.R
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val MIN_TTS_SPEED = 0.1f
const val MAX_TTS_SPEED = 5.0f

object TtsEngine {
    val isInitializingState = mutableStateOf(false)
    val isInitializedState = mutableStateOf(false)

    var supertonicTts: OfflineTts? = null

    var tts: OfflineTts?
        get() = supertonicTts
        set(value) {
            // Backwards compatibility
        }

    // https://en.wikipedia.org/wiki/ISO_639-3
    // Example:
    // eng for English,
    // deu for German
    // cmn for Mandarin
    var lang: String? = null

    // if a model supports two languages, set also lang2
    var lang2: String? = null

    // for Supertonic TTS: language code in ISO 639-1 format, e.g., "en", "zh", "ja"
    val supertonicLangState: MutableState<String> = mutableStateOf("auto")

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

    val isSupertonic: Boolean
        get() = currentModel.isSupertonic



    init {
        // Models are now dynamically loaded via PreferenceHelper and Models object
    }

    fun createTts(context: Context, onComplete: (() -> Unit)? = null) {
        Log.i(TAG, "Init Next-gen Kaldi TTS")
        if (supertonicTts == null) {
            updateTts(context, onComplete)
        } else {
            isInitializedState.value = true
            onComplete?.invoke()
        }
    }

    fun updateTts(context: Context, onComplete: (() -> Unit)? = null) {
        isInitializingState.value = true
        isInitializedState.value = false
        Toast.makeText(context.applicationContext, context.getString(R.string.toast_tts_initializing), Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            initTts(context)
            withContext(Dispatchers.Main) {
                isInitializingState.value = false
                val success = supertonicTts != null
                isInitializedState.value = success
                if (success) {
                    Toast.makeText(context.applicationContext, context.getString(R.string.toast_tts_initialized), Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                } else {
                    Toast.makeText(context.applicationContext, context.getString(R.string.toast_tts_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initTts(context: Context) {
        val preferenceHelper = PreferenceHelper(context)

        // Only initialize supertonic-3-tts
        try {
            Log.i(TAG, "Initializing supertonic-3-tts...")
            val (engine, config) = loadModel(context, "supertonic-3-tts")
            supertonicTts = engine
            currentModel = config
            lang = config.lang
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize supertonic-3-tts", e)
        }

        speed = preferenceHelper.getSpeed()
        speakerId = preferenceHelper.getSid()
        supertonicLang = preferenceHelper.getLanguage("auto")

        // Ensure lang is never null or empty after init, so TtsService doesn't crash
        if (lang == null || lang!!.isEmpty()) {
            lang = "eng"
        }
    }

    private fun loadModel(context: Context, modelId: String): Pair<OfflineTts?, ModelConfig> {
        val config = Models.getModel(modelId)

        if (!validateModelAssets(context, config)) {
            throw IllegalStateException(
                "Model '${config.name}' is missing required asset files. " +
                "Cannot initialize safely."
            )
        }

        var currentDataDir = config.dataDir
        if (currentDataDir.isNotEmpty()) {
            val newDir = copyDataDir(context, currentDataDir)
            currentDataDir = "$newDir/$currentDataDir"
        }

        var currentDictDir = config.dictDir
        if (currentDictDir.isNotEmpty()) {
            val newDir = copyDataDir(context, currentDictDir)
            currentDictDir = "$newDir/$currentDictDir"
        }

        val ttsConfig = getOfflineTtsConfig(
            modelDir = config.modelDir,
            modelName = config.modelName,
            acousticModelName = config.acousticModelName,
            vocoder = config.vocoder,
            voices = config.voices,
            lexicon = config.lexicon,
            dataDir = currentDataDir,
            dictDir = currentDictDir,
            ruleFsts = config.ruleFsts,
            ruleFars = "",
            isKitten = false,
            isSupertonic = config.isSupertonic,
            durationPredictor = config.durationPredictor,
            textEncoder = config.textEncoder,
            vectorEstimator = config.vectorEstimator,
            supertonicVocoder = config.supertonicVocoder,
            ttsJson = config.ttsJson,
            unicodeIndexer = config.unicodeIndexer,
            voiceStyle = config.voiceStyle,
        )

        val engine = OfflineTts(assetManager = context.assets, config = ttsConfig)
        return Pair(engine, config)
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

class RealtimeResampler(val factor: Float) {
    private var lastFraction = 0.0f

    fun process(input: FloatArray): FloatArray {
        if (factor == 1.0f || factor <= 0.0f || input.isEmpty()) return input
        
        val inputSize = input.size
        val estimatedSize = (inputSize / factor).toInt() + 2
        val tempOutput = FloatArray(estimatedSize)
        var outIdx = 0
        
        var pos = lastFraction
        while (pos < inputSize) {
            val idx = pos.toInt()
            val frac = pos - idx
            val sample = if (idx + 1 < inputSize) {
                input[idx] * (1.0f - frac) + input[idx + 1] * frac
            } else {
                input[idx]
            }
            if (outIdx < tempOutput.size) {
                tempOutput[outIdx++] = sample
            }
            pos += factor
        }
        
        lastFraction = pos - inputSize
        return tempOutput.copyOfRange(0, outIdx)
    }
}
