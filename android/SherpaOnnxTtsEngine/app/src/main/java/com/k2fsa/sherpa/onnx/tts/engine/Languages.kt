package com.k2fsa.sherpa.onnx.tts.engine

object Languages {
    data class Language(val code: String, val name: String)

    val supportedLanguages = listOf(
        Language("en", "English"),
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
            "en" -> "eng"
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
}
