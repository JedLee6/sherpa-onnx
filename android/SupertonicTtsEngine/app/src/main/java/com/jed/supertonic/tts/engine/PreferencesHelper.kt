import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    private val PREFS_NAME = "com.jed.supertonic.tts.engine"
    private val SPEED_KEY = "speed"
    private val SID_KEY = "speaker_id"
    private val LANG_KEY = "language"
    private val MODEL_KEY = "model_id"
    private val MATCHA_PITCH_KEY = "matcha_pitch"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setSpeed(value: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat(SPEED_KEY, value)
        editor.apply()
    }

    fun getSpeed(): Float {
        return sharedPreferences.getFloat(SPEED_KEY, 1.0f)
    }

    fun setSid(value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(SID_KEY, value)
        editor.apply()
    }

    fun getSid(): Int {
        return sharedPreferences.getInt(SID_KEY, 0)
    }

    fun setLanguage(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(LANG_KEY, value)
        editor.apply()
    }

    fun getLanguage(defaultValue: String = "en"): String {
        return sharedPreferences.getString(LANG_KEY, defaultValue) ?: defaultValue
    }

    fun setModel(value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(MODEL_KEY, value)
        editor.apply()
    }

    fun getModel(defaultValue: String = "supertonic-3-tts"): String {
        return sharedPreferences.getString(MODEL_KEY, defaultValue) ?: defaultValue
    }

    fun setMatchaPitch(value: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat(MATCHA_PITCH_KEY, value)
        editor.apply()
    }

    fun getMatchaPitch(defaultValue: Float = 0.85f): Float {
        return sharedPreferences.getFloat(MATCHA_PITCH_KEY, defaultValue)
    }
}