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

    fun mapLinguaToIso1(language: com.github.pemistahl.lingua.api.Language): String? {
        return when (language) {
            com.github.pemistahl.lingua.api.Language.ENGLISH -> "en"
            com.github.pemistahl.lingua.api.Language.KOREAN -> "ko"
            com.github.pemistahl.lingua.api.Language.JAPANESE -> "ja"
            com.github.pemistahl.lingua.api.Language.ARABIC -> "ar"
            com.github.pemistahl.lingua.api.Language.BULGARIAN -> "bg"
            com.github.pemistahl.lingua.api.Language.CZECH -> "cs"
            com.github.pemistahl.lingua.api.Language.DANISH -> "da"
            com.github.pemistahl.lingua.api.Language.GERMAN -> "de"
            com.github.pemistahl.lingua.api.Language.GREEK -> "el"
            com.github.pemistahl.lingua.api.Language.SPANISH -> "es"
            com.github.pemistahl.lingua.api.Language.ESTONIAN -> "et"
            com.github.pemistahl.lingua.api.Language.FINNISH -> "fi"
            com.github.pemistahl.lingua.api.Language.FRENCH -> "fr"
            com.github.pemistahl.lingua.api.Language.HINDI -> "hi"
            com.github.pemistahl.lingua.api.Language.CROATIAN -> "hr"
            com.github.pemistahl.lingua.api.Language.HUNGARIAN -> "hu"
            com.github.pemistahl.lingua.api.Language.INDONESIAN -> "id"
            com.github.pemistahl.lingua.api.Language.ITALIAN -> "it"
            com.github.pemistahl.lingua.api.Language.LITHUANIAN -> "lt"
            com.github.pemistahl.lingua.api.Language.LATVIAN -> "lv"
            com.github.pemistahl.lingua.api.Language.DUTCH -> "nl"
            com.github.pemistahl.lingua.api.Language.POLISH -> "pl"
            com.github.pemistahl.lingua.api.Language.PORTUGUESE -> "pt"
            com.github.pemistahl.lingua.api.Language.ROMANIAN -> "ro"
            com.github.pemistahl.lingua.api.Language.RUSSIAN -> "ru"
            com.github.pemistahl.lingua.api.Language.SLOVAK -> "sk"
            com.github.pemistahl.lingua.api.Language.SLOVENE -> "sl"
            com.github.pemistahl.lingua.api.Language.SWEDISH -> "sv"
            com.github.pemistahl.lingua.api.Language.TURKISH -> "tr"
            com.github.pemistahl.lingua.api.Language.UKRAINIAN -> "uk"
            com.github.pemistahl.lingua.api.Language.VIETNAMESE -> "vi"
            com.github.pemistahl.lingua.api.Language.CHINESE -> "zh"
            else -> null
        }
    }
}
