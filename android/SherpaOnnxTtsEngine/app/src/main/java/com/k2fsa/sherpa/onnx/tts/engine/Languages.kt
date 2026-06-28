package com.k2fsa.sherpa.onnx.tts.engine

object Languages {
    data class Language(val code: String, val name: String)

    val supportedLanguages = listOf(
        Language("auto", "Auto-Detect"),
        Language("en", "English"),
        Language("zh", "Chinese"),
        Language("ko", "Korean"),
        Language("ja", "Japanese"),
        Language("ar", "Arabic"),
        Language("bg", "Bulgarian"),
        Language("cs", "Czech"),
        Language("da", "Danish"),
        Language("de", "German"),
        Language("el", "Greek"),
        Language("es", "Spanish"),
        Language("et", "Estonian"),
        Language("fi", "Finnish"),
        Language("fr", "French"),
        Language("hi", "Hindi"),
        Language("hr", "Croatian"),
        Language("hu", "Hungarian"),
        Language("id", "Indonesian"),
        Language("it", "Italian"),
        Language("lt", "Lithuanian"),
        Language("lv", "Latvian"),
        Language("nl", "Dutch"),
        Language("pl", "Polish"),
        Language("pt", "Portuguese"),
        Language("ro", "Romanian"),
        Language("ru", "Russian"),
        Language("sk", "Slovak"),
        Language("sl", "Slovenian"),
        Language("sv", "Swedish"),
        Language("tr", "Turkish"),
        Language("uk", "Ukrainian"),
        Language("vi", "Vietnamese")
    )

    fun getName(code: String): String {
        return supportedLanguages.find { it.code == code }?.name ?: "Unknown"
    }

    fun getIso3Code(iso1: String): String {
        return when (iso1) {
            "auto" -> "eng"
            "en" -> "eng"
            "zh" -> "zho"
            "ko" -> "kor"
            "ja" -> "jpn"
            "ar" -> "ara"
            "bg" -> "bul"
            "cs" -> "ces"
            "da" -> "dan"
            "de" -> "deu"
            "el" -> "ell"
            "es" -> "spa"
            "et" -> "est"
            "fi" -> "fin"
            "fr" -> "fra"
            "hi" -> "hin"
            "hr" -> "hrv"
            "hu" -> "hun"
            "id" -> "ind"
            "it" -> "ita"
            "lt" -> "lit"
            "lv" -> "lav"
            "nl" -> "nld"
            "pl" -> "pol"
            "pt" -> "por"
            "ro" -> "ron"
            "ru" -> "rus"
            "sk" -> "slk"
            "sl" -> "slv"
            "sv" -> "swe"
            "tr" -> "tur"
            "uk" -> "ukr"
            "vi" -> "vie"
            else -> iso1
        }
    }

    fun mapGoogleMlKitToIso1(code: String?): String? {
        if (code == null || code == "und") return null
        val lowerCode = code.lowercase()
        return when {
            lowerCode.startsWith("zh") -> "zh"
            else -> lowerCode
        }
    }

    fun detectCjkLanguage(text: String): String? {
        var hasKana = false
        var hasHangul = false
        var hasHan = false

        for (i in 0 until text.length) {
            val c = text[i]
            val codePoint = c.code
            when {
                codePoint in 0x3040..0x309F || codePoint in 0x30A0..0x30FF || 
                codePoint in 0x31F0..0x31FF || codePoint in 0xFF65..0xFF9F -> {
                    hasKana = true
                }
                codePoint in 0xAC00..0xD7A3 || codePoint in 0x1100..0x11FF || 
                codePoint in 0x3130..0x318F -> {
                    hasHangul = true
                }
                codePoint in 0x4E00..0x9FFF || codePoint in 0x3400..0x4DBF -> {
                    hasHan = true
                }
            }
        }

        return when {
            hasKana -> "ja"
            hasHangul -> "ko"
            hasHan -> "zh"
            else -> null
        }
    }
}
