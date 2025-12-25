package com.k2fsa.sherpa.onnx

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var modelTypeSpinner: Spinner
    private lateinit var languageSpinner: Spinner
    private lateinit var saveButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val PREFS_NAME = "AppSettings"
        const val MODEL_TYPE_KEY = "model_type"
        const val WHISPER_LANGUAGE_KEY = "whisper_language"
        
        // 定义模型类型选项
        val MODEL_TYPES = arrayOf(
            "Whisper Large-v3 (type=3)",
            "sherpa-onnx-fire-red-asr-large-zh_en-2025-02-16 (type=24)",
            "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2025-09-09 (type=41)",
            "sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8 (type=40)",
            "Whisper Tiny-en (type=2)",
            "Paraformer Chinese (type=0)",
            "Zipformer English (type=1)",
            "Zipformer Chinese (type=4)",
        )
        
        val MODEL_TYPE_VALUES = intArrayOf(3, 24, 41, 40, 2, 0, 1, 4)

        // LANGUAGES字典
        val LANGUAGES = mapOf(
            "auto_detect" to "",
            "en" to "english",
            "zh" to "chinese", 
            "de" to "german",
            "es" to "spanish",
            "ru" to "russian",
            "ko" to "korean",
            "fr" to "french",
            "ja" to "japanese",
            "pt" to "portuguese",
            "tr" to "turkish",
            "pl" to "polish",
            "ca" to "catalan",
            "nl" to "dutch",
            "ar" to "arabic",
            "sv" to "swedish",
            "it" to "italian",
            "id" to "indonesian",
            "hi" to "hindi",
            "fi" to "finnish",
            "vi" to "vietnamese",
            "he" to "hebrew",
            "uk" to "ukrainian",
            "el" to "greek",
            "ms" to "malay",
            "cs" to "czech",
            "ro" to "romanian",
            "da" to "danish",
            "hu" to "hungarian",
            "ta" to "tamil",
            "no" to "norwegian",
            "th" to "thai",
            "ur" to "urdu",
            "hr" to "croatian",
            "bg" to "bulgarian",
            "lt" to "lithuanian",
            "la" to "latin",
            "mi" to "maori",
            "ml" to "malayalam",
            "cy" to "welsh",
            "sk" to "slovak",
            "te" to "telugu",
            "fa" to "persian",
            "lv" to "latvian",
            "bn" to "bengali",
            "sr" to "serbian",
            "az" to "azerbaijani",
            "sl" to "slovenian",
            "kn" to "kannada",
            "et" to "estonian",
            "mk" to "macedonian",
            "br" to "breton",
            "eu" to "basque",
            "is" to "icelandic",
            "hy" to "armenian",
            "ne" to "nepali",
            "mn" to "mongolian",
            "bs" to "bosnian",
            "kk" to "kazakh",
            "sq" to "albanian",
            "sw" to "swahili",
            "gl" to "galician",
            "mr" to "marathi",
            "pa" to "punjabi",
            "si" to "sinhala",
            "km" to "khmer",
            "sn" to "shona",
            "yo" to "yoruba",
            "so" to "somali",
            "af" to "afrikaans",
            "oc" to "occitan",
            "ka" to "georgian",
            "be" to "belarusian",
            "tg" to "tajik",
            "sd" to "sindhi",
            "gu" to "gujarati",
            "am" to "amharic",
            "yi" to "yiddish",
            "lo" to "lao",
            "uz" to "uzbek",
            "fo" to "faroese",
            "ht" to "haitian creole",
            "ps" to "pashto",
            "tk" to "turkmen",
            "nn" to "nynorsk",
            "mt" to "maltese",
            "sa" to "sanskrit",
            "lb" to "luxembourgish",
            "my" to "myanmar",
            "bo" to "tibetan",
            "tl" to "tagalog",
            "mg" to "malagasy",
            "as" to "assamese",
            "tt" to "tatar",
            "haw" to "hawaiian",
            "ln" to "lingala",
            "ha" to "hausa",
            "ba" to "bashkir",
            "jw" to "javanese",
            "su" to "sundanese",
            "yue" to "cantonese",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        modelTypeSpinner = findViewById(R.id.model_type_spinner)
        languageSpinner = findViewById(R.id.language_spinner)
        saveButton = findViewById(R.id.save_button)

        setupModelTypeSpinner()
        setupLanguageSpinner()
        loadCurrentSettings()

        saveButton.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupModelTypeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, MODEL_TYPES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelTypeSpinner.adapter = adapter
    }

    private fun setupLanguageSpinner() {
        val languageOptions = ArrayList<String>()
        languageOptions.add("auto_detect (Auto Detect)")
        LANGUAGES.filter { it.key != "auto_detect" }.forEach { (code, name) ->
            languageOptions.add("$code ($name)")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
    }

    private fun loadCurrentSettings() {
        val currentModelType = sharedPreferences.getInt(MODEL_TYPE_KEY, 3) // 默认whisper large-v3
        val currentLanguage = sharedPreferences.getString(WHISPER_LANGUAGE_KEY, "auto_detect") ?: "auto_detect"

        // 设置模型类型选择
        val modelTypeIndex = MODEL_TYPE_VALUES.indexOf(currentModelType)
        if (modelTypeIndex != -1) {
            modelTypeSpinner.setSelection(modelTypeIndex)
        }

        // 设置语言选择
        val languageDisplay = if (currentLanguage == "auto_detect") {
            "auto_detect (Auto Detect)"
        } else {
            val languageName = LANGUAGES[currentLanguage] ?: currentLanguage
            "$currentLanguage ($languageName)"
        }
        val languageIndex = (0 until languageSpinner.adapter.count).indexOfFirst { 
            languageSpinner.adapter.getItem(it).toString() == languageDisplay 
        }
        if (languageIndex != -1) {
            languageSpinner.setSelection(languageIndex)
        }
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()
        
        val selectedModelTypeIndex = modelTypeSpinner.selectedItemPosition
        val selectedModelType = if (selectedModelTypeIndex >= 0 && selectedModelTypeIndex < MODEL_TYPE_VALUES.size) {
            MODEL_TYPE_VALUES[selectedModelTypeIndex]
        } else {
            3 // 默认值
        }
        editor.putInt(MODEL_TYPE_KEY, selectedModelType)

        val selectedLanguageText = languageSpinner.selectedItem.toString()
        val selectedLanguageCode = if (selectedLanguageText.startsWith("auto_detect")) {
            ""
        } else {
            selectedLanguageText.substringBefore(" ").trim()
        }
        editor.putString(WHISPER_LANGUAGE_KEY, selectedLanguageCode)

        editor.apply()
    }
}