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
    val supertonicLang: String = ""
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
            supertonicLang = "en"
        ),
        ModelConfig(
            id = "vits-piper-xiao_ya",
            name = "xiao_ya-medium (VITS)",
            modelDir = "vits-piper-zh_CN-xiao_ya-medium",
            modelName = "zh_CN-xiao_ya-medium.onnx",
            lexicon = "lexicon.txt",
            lang = "zho"
        ),
        ModelConfig(
            id = "vits-piper-chaowen",
            name = "chaowen-medium (VITS)",
            modelDir = "vits-piper-zh_CN-chaowen-medium",
            modelName = "zh_CN-chaowen-medium.onnx",
            lexicon = "lexicon.txt",
            lang = "zho"
        ),
        ModelConfig(
            id = "matcha-icefall-zh-baker",
            name = "zh-baker (Matcha)",
            modelDir = "matcha-icefall-zh-baker",
            acousticModelName = "model-steps-3.onnx",
            vocoder = "vocos-22khz-univ.onnx",
            lexicon = "lexicon.txt",
            lang = "zho"
        )
    )

    fun getModel(id: String): ModelConfig {
        return supportedModels.find { it.id == id } ?: supportedModels[0]
    }
}
