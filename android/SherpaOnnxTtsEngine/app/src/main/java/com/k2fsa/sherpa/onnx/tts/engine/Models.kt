package com.k2fsa.sherpa.onnx.tts.engine

data class ModelConfig(
    val id: String,
    val name: String,
    val modelDir: String,
    val modelName: String = "",
    val acousticModelName: String = "",
    val vocoder: String = "",
    val voices: String = "",
    val dataDir: String = "",
    val lexicon: String = "",
    val lang: String = "",
    val isSupertonic: Boolean = false,
    val durationPredictor: String = "",
    val textEncoder: String = "",
    val vectorEstimator: String = "",
    val supertonicVocoder: String = "",
    val ttsJson: String = "",
    val unicodeIndexer: String = "",
    val voiceStyle: String = "",
    val supertonicLang: String = "",
    val dictDir: String = "",
    val ruleFsts: String = "",
    // List of asset file paths (relative to assets/) that must exist for this model to load.
    // Used for pre-validation before native initialization.
    val requiredFiles: List<String> = emptyList()
)

object Models {
    val supportedModels = listOf(
        ModelConfig(
            id = "supertonic-3-tts",
            name = "supertonic-3-tts",
            modelDir = "sherpa-onnx-supertonic-3-tts-int8-2026-05-11",
            isSupertonic = true,
            durationPredictor = "duration_predictor.int8.onnx",
            textEncoder = "text_encoder.int8.onnx",
            vectorEstimator = "vector_estimator.int8.onnx",
            supertonicVocoder = "vocoder.int8.onnx",
            ttsJson = "tts.json",
            unicodeIndexer = "unicode_indexer.bin",
            voiceStyle = "voice.bin",
            supertonicLang = "en",
            requiredFiles = listOf(
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/duration_predictor.int8.onnx",
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/text_encoder.int8.onnx",
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/vector_estimator.int8.onnx",
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/vocoder.int8.onnx",
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/tts.json",
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/unicode_indexer.bin",
                "sherpa-onnx-supertonic-3-tts-int8-2026-05-11/voice.bin",
            )
        )
    )

    fun getModel(id: String): ModelConfig {
        return supportedModels.find { it.id == id } ?: supportedModels[0]
    }
}
