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
        ),
        ModelConfig(
            id = "vits-piper-xiao_ya",
            name = "xiao_ya-medium (VITS)",
            modelDir = "vits-piper-zh_CN-xiao_ya-medium",
            modelName = "zh_CN-xiao_ya-medium.onnx",
            lexicon = "lexicon.txt",
            lang = "zho",
            requiredFiles = listOf(
                "vits-piper-zh_CN-xiao_ya-medium/zh_CN-xiao_ya-medium.onnx",
                "vits-piper-zh_CN-xiao_ya-medium/lexicon.txt",
                "vits-piper-zh_CN-xiao_ya-medium/tokens.txt",
            )
        ),
        ModelConfig(
            id = "vits-piper-chaowen",
            name = "chaowen-medium (VITS)",
            modelDir = "vits-piper-zh_CN-chaowen-medium",
            modelName = "zh_CN-chaowen-medium.onnx",
            lexicon = "lexicon.txt",
            lang = "zho",
            requiredFiles = listOf(
                "vits-piper-zh_CN-chaowen-medium/zh_CN-chaowen-medium.onnx",
                "vits-piper-zh_CN-chaowen-medium/lexicon.txt",
                "vits-piper-zh_CN-chaowen-medium/tokens.txt",
            )
        ),
        ModelConfig(
            id = "matcha-icefall-zh-baker",
            name = "zh-baker (Matcha)",
            modelDir = "matcha-icefall-zh-baker",
            acousticModelName = "model-steps-3.onnx",
            vocoder = "vocos-22khz-univ.onnx",
            lexicon = "lexicon.txt",
            lang = "zho",
            dictDir = "matcha-icefall-zh-baker/dict",
            ruleFsts = "matcha-icefall-zh-baker/phone.fst,matcha-icefall-zh-baker/date.fst,matcha-icefall-zh-baker/number.fst",
            requiredFiles = listOf(
                "matcha-icefall-zh-baker/model-steps-3.onnx",
                "matcha-icefall-zh-baker/lexicon.txt",
                "matcha-icefall-zh-baker/tokens.txt",
                "vocos-22khz-univ.onnx",
                "matcha-icefall-zh-baker/phone.fst",
                "matcha-icefall-zh-baker/date.fst",
                "matcha-icefall-zh-baker/number.fst",
                "matcha-icefall-zh-baker/dict/jieba.dict.utf8",
                "matcha-icefall-zh-baker/dict/hmm_model.utf8"
            )
        )
    )

    fun getModel(id: String): ModelConfig {
        return supportedModels.find { it.id == id } ?: supportedModels[0]
    }
}
