@file:OptIn(ExperimentalMaterial3Api::class)

package com.k2fsa.sherpa.onnx.tts.engine

import PreferenceHelper
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.tts.engine.ui.theme.SherpaOnnxTtsEngineTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.TimeSource
import java.text.BreakIterator
import java.util.Locale
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.languagedetector.LanguageDetector
import com.google.mediapipe.tasks.text.languagedetector.LanguageDetector.LanguageDetectorOptions

const val TAG = "sherpa-onnx-tts-engine"

class AudioChunk(val samples: FloatArray, val sampleRate: Int)

class MainActivity : ComponentActivity() {
    private var languageDetector: LanguageDetector? = null
    // TODO(fangjun): Save settings in ttsViewModel
    private val ttsViewModel: TtsViewModel by viewModels()

    private var mediaPlayer: MediaPlayer? = null

    // see
    // https://developer.android.com/reference/kotlin/android/media/AudioTrack
    private lateinit var track: AudioTrack

    private var stopped: Boolean = false

    private var samplesChannel = Channel<AudioChunk>(capacity = 128)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeResampler: RealtimeResampler? = null
    private var activeSampleRate: Int = 22050


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("language_detector.tflite")
                .build()
            val options = LanguageDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            languageDetector = LanguageDetector.createFromOptions(this, options)
            Log.i(TAG, "MediaPipe Language Detector initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe Language Detector", e)
        }

        Log.i(TAG, "Start to initialize TTS")
        TtsEngine.createTts(this) {
            Log.i(TAG, "Finish initializing TTS")
            Log.i(TAG, "Start to initialize AudioTrack")
            initAudioTrack()
            Log.i(TAG, "Finish initializing AudioTrack")
            activeSampleRate = TtsEngine.tts!!.sampleRate()
        }

        val preferenceHelper = PreferenceHelper(this)
        setContent {
            SherpaOnnxTtsEngineTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(topBar = {
                        TopAppBar(title = { Text(stringResource(R.string.app_bar_title)) })
                    }) {
                        Box(modifier = Modifier.padding(it)) {
                            val context = LocalContext.current
                            val mainScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(mainScrollState)
                            ) {
                                var expandedModel by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expandedModel && !TtsEngine.isInitializingState.value,
                                    onExpandedChange = {
                                        if (!TtsEngine.isInitializingState.value) {
                                            expandedModel = !expandedModel
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = TtsEngine.currentModel.name,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.model_label)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = expandedModel
                                            )
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedModel,
                                        onDismissRequest = { expandedModel = false }
                                    ) {
                                        Models.supportedModels.forEach { model ->
                                            DropdownMenuItem(
                                                text = { Text(model.name) },
                                                onClick = {
                                                    if (TtsEngine.validateModelAssets(context, model)) {
                                                        preferenceHelper.setModel(model.id)
                                                        TtsEngine.updateTts(context) {
                                                            initAudioTrack()
                                                        }
                                                    } else {
                                                         Toast.makeText(
                                                             context,
                                                             context.getString(R.string.toast_model_missing_files, model.name),
                                                             Toast.LENGTH_LONG
                                                         ).show()
                                                    }
                                                    expandedModel = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Column {
                                     Text(stringResource(R.string.speed_label, TtsEngine.speed))
                                    Slider(
                                        value = TtsEngine.speedState.value,
                                        onValueChange = {
                                            TtsEngine.speed = it
                                            preferenceHelper.setSpeed(it)
                                        },
                                        valueRange = MIN_TTS_SPEED..MAX_TTS_SPEED,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (TtsEngine.isSupertonic) {
                                    var expanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expanded && !TtsEngine.isInitializingState.value,
                                        onExpandedChange = {
                                            if (!TtsEngine.isInitializingState.value) {
                                                expanded = !expanded
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = Languages.getName(TtsEngine.supertonicLang),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.language_label)) },
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                    expanded = expanded
                                                )
                                            },
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            Languages.supportedLanguages.forEach { language ->
                                                DropdownMenuItem(
                                                    text = { Text(language.name) },
                                                    onClick = {
                                                        TtsEngine.supertonicLang = language.code
                                                        preferenceHelper.setLanguage(language.code)
                                                        expanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }


                                var testText by remember { mutableStateOf("Hi. Nice to meet you, I'm Jed. Hallo. Freut mich, dich kennenzulernen, ich bin Jed. Bonjour. Ravi de te rencontrer, je suis Jed. Hola. Encantado de conocerte, soy Jed. Ciao. Piacere di conoscerti, sono Jed.") }

                                var startEnabled by remember { mutableStateOf(true) }
                                var playEnabled by remember { mutableStateOf(false) }
                                var saveEnabled by remember { mutableStateOf(false) }
                                var shareEnabled by remember { mutableStateOf(false) }
                                var rtfText by remember {
                                    mutableStateOf("")
                                }
                                var detectedLanguagesText by remember {
                                    mutableStateOf("")
                                }

                                val saveLauncher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.CreateDocument("audio/wav")
                                ) { uri ->
                                    if (uri != null) {
                                        try {
                                            val srcFile = File(application.filesDir.absolutePath + "/generated.wav")
                                            contentResolver.openOutputStream(uri)?.use { output ->
                                                srcFile.inputStream().use { input ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            Toast.makeText(applicationContext, getString(R.string.toast_audio_saved), Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to save audio: $e")
                                            Toast.makeText(applicationContext, getString(R.string.toast_audio_save_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

                                if (TtsEngine.isInitializedState.value && TtsEngine.tts != null) {
                                    val numSpeakers = TtsEngine.tts!!.numSpeakers()
                                    if (numSpeakers > 1) {
                                        OutlinedTextField(
                                            value = TtsEngine.speakerIdState.value.toString(),
                                            onValueChange = {
                                                if (it.isEmpty() || it.isBlank()) {
                                                    TtsEngine.speakerId = 0
                                                } else {
                                                    try {
                                                        TtsEngine.speakerId = it.toString().toInt()
                                                    } catch (ex: NumberFormatException) {
                                                        Log.i(TAG, "Invalid input: $it")
                                                        TtsEngine.speakerId = 0
                                                    }
                                                }
                                                preferenceHelper.setSid(TtsEngine.speakerId)
                                            },
                                            label = {
                                                Text(stringResource(R.string.speaker_id_label, numSpeakers - 1))
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                                .wrapContentHeight(),
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = testText,
                                    onValueChange = { testText = it },
                                    label = { Text(stringResource(R.string.input_label)) },
                                    maxLines = 10,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .wrapContentHeight(),
                                    singleLine = false,
                                )

                                Row {
                                    Button(
                                        enabled = startEnabled && TtsEngine.isInitializedState.value && !TtsEngine.isInitializingState.value,
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            Log.i(TAG, "Clicked, text: $testText")
                                            if (testText.isBlank() || testText.isEmpty()) {
                                                 Toast.makeText(
                                                     applicationContext,
                                                     getString(R.string.toast_please_input_text),
                                                     Toast.LENGTH_SHORT
                                                 ).show()
                                            } else {
                                                startEnabled = false
                                                playEnabled = false
                                                saveEnabled = false
                                                shareEnabled = false
                                                stopped = false

                                                track.pause()
                                                track.flush()
                                                track.play()
                                                rtfText = ""
                                                detectedLanguagesText = ""
                                                Log.i(TAG, "Started with text $testText")

                                                scope.launch {
                                                    for (chunk in samplesChannel) {
                                                        val samples = chunk.samples
                                                        if (samples.isEmpty()) {
                                                            break
                                                        }

                                                        if (track.playbackRate != chunk.sampleRate) {
                                                            track.playbackRate = chunk.sampleRate
                                                        }

                                                        Log.i(
                                                            TAG,
                                                            "Received ${samples.count()} samples"
                                                        )
                                                        track.write(
                                                            samples,
                                                            0,
                                                            samples.size,
                                                            AudioTrack.WRITE_BLOCKING
                                                        )
                                                        if (stopped) {
                                                            break
                                                        }
                                                    }
                                                    Log.i(TAG, "Draining the channel")

                                                    // drain remaining
                                                    while (!samplesChannel.isEmpty) {
                                                        samplesChannel.tryReceive().getOrNull()
                                                    }
                                                    Log.i(TAG, "Channel drained")

                                                }

                                                CoroutineScope(Dispatchers.Default).launch {
                                                    val timeSource = TimeSource.Monotonic
                                                    val startTime = timeSource.markNow()
                                                    val sampleRate = TtsEngine.tts!!.sampleRate()
                                                    val allSamples = mutableListOf<FloatArray>()

                                                    if (TtsEngine.isSupertonic && TtsEngine.supertonicLang == "auto") {
                                                        val sentences = TextSegmenter.splitText(testText)
                                                        val sentencesInfo = mutableListOf<String>()

                                                        for (sentence in sentences) {
                                                            if (stopped) break
                                                             val cjkLang = Languages.detectCjkLanguage(sentence)
                                                             val iso1 = if (cjkLang != null) {
                                                                 Log.i(TAG, "Sentence: '$sentence', CJK language detected directly: $cjkLang")
                                                                 cjkLang
                                                             } else {
                                                                  val detected = try {
                                                                      val result = languageDetector?.detect(sentence)
                                                                      val prediction = result?.languagesAndScores()?.firstOrNull()
                                                                      prediction?.languageCode() ?: "und"
                                                                  } catch (e: Exception) {
                                                                      Log.e(TAG, "Language identification failed", e)
                                                                      "und"
                                                                  }
                                                                  Languages.mapMediaPipeToIso1(detected) ?: "en"
                                                             }
                                                             Log.i(TAG, "Sentence: '$sentence', final mapped iso1: $iso1")
                                                             sentencesInfo.add("[$iso1] $sentence")
                                                             val currentInfo = sentencesInfo.joinToString("\n")
                                                             withContext(Dispatchers.Main) {
                                                                 detectedLanguagesText = currentInfo
                                                             }

                                                              val selectedTts = TtsEngine.supertonicTts ?: TtsEngine.tts!!

                                                              val nativeRate = TtsEngine.tts!!.sampleRate()
                                                              val generatorRate = selectedTts.sampleRate()
                                                              val factor = generatorRate.toFloat() / nativeRate

                                                              activeSampleRate = nativeRate
                                                              activeResampler = if (factor != 1.0f) {
                                                                  RealtimeResampler(factor)
                                                              } else {
                                                                  null
                                                              }
                                                              val targetSpeed = TtsEngine.speed
                                                              Log.i(TAG, "Sentence loop debug - sentence: '$sentence', selectedTts: $selectedTts, nativeRate: $nativeRate, generatorRate: $generatorRate, factor: $factor, activeResampler: $activeResampler, speed: ${TtsEngine.speed}, targetSpeed: $targetSpeed")
                                                              val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = targetSpeed)
                                                              genConfig.extra = mapOf("lang" to iso1)

                                                              val audio = selectedTts.generateWithConfigAndCallback(
                                                                  text = sentence,
                                                                  config = genConfig,
                                                                  callback = ::callback,
                                                              )
                                                              val processedSamples = if (activeResampler != null) {
                                                                  RealtimeResampler(factor).process(audio.samples)
                                                              } else {
                                                                  audio.samples
                                                              }
                                                              allSamples.add(processedSamples)
                                                         }
                                                    } else {
                                                         val selectedTts = TtsEngine.tts!!
                                                         val nativeRate = selectedTts.sampleRate()
                                                         val generatorRate = selectedTts.sampleRate()
                                                         val factor = generatorRate.toFloat() / nativeRate

                                                         activeSampleRate = nativeRate
                                                         activeResampler = if (factor != 1.0f) {
                                                             RealtimeResampler(factor)
                                                         } else {
                                                             null
                                                         }
                                                         val targetSpeed = TtsEngine.speed
                                                         Log.i(TAG, "Else branch debug - testText: '$testText', selectedTts: $selectedTts, nativeRate: $nativeRate, generatorRate: $generatorRate, factor: $factor, activeResampler: $activeResampler, speed: ${TtsEngine.speed}, targetSpeed: $targetSpeed")
                                                         val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = targetSpeed)
                                                         if (TtsEngine.isSupertonic) {
                                                             genConfig.extra = mapOf("lang" to TtsEngine.supertonicLang)
                                                         }
                                                         val audio =
                                                             selectedTts.generateWithConfigAndCallback(
                                                                 text = testText,
                                                                 config = genConfig,
                                                                 callback = ::callback,
                                                             )
                                                         val processedSamples = if (activeResampler != null) {
                                                             RealtimeResampler(factor).process(audio.samples)
                                                         } else {
                                                             audio.samples
                                                         }
                                                          allSamples.add(processedSamples)
                                                          withContext(Dispatchers.Main) {
                                                              detectedLanguagesText = ""
                                                          }
                                                    }

                                                    val elapsed =
                                                        startTime.elapsedNow().inWholeMilliseconds.toFloat() / 1000

                                                    var totalSamplesCount = 0
                                                    for (s in allSamples) totalSamplesCount += s.size
                                                    val combinedSamples = FloatArray(totalSamplesCount)
                                                    var offset = 0
                                                    for (s in allSamples) {
                                                        s.copyInto(combinedSamples, offset)
                                                        offset += s.size
                                                    }

                                                    val audioDuration =
                                                        combinedSamples.size / sampleRate.toFloat()
                                                     val RTF = this@MainActivity.getString(
                                                         R.string.rtf_format,
                                                         TtsEngine.tts!!.config.model.numThreads,
                                                         elapsed,
                                                         audioDuration,
                                                         elapsed,
                                                         audioDuration,
                                                         if (audioDuration > 0) elapsed / audioDuration else 0f
                                                     )

                                                    scope.launch {
                                                        Log.i(TAG, "send 0 samples")
                                                             samplesChannel.send(AudioChunk(FloatArray(0), 22050))
                                                        Log.i(TAG, "send 0 samples done")
                                                    }

                                                    val filename =
                                                        application.filesDir.absolutePath + "/generated.wav"

                                                    val combinedAudio = com.k2fsa.sherpa.onnx.GeneratedAudio(combinedSamples, sampleRate)
                                                    val ok = combinedSamples.isNotEmpty() && combinedAudio.save(filename)

                                                    withContext(Dispatchers.Main) {
                                                        startEnabled = true
                                                        if (ok) {
                                                            playEnabled = true
                                                            saveEnabled = true
                                                            shareEnabled = true
                                                        }
                                                        rtfText = RTF
                                                    }
                                                }
                                            }
                                        }) {
                                         Text(stringResource(R.string.btn_start))
                                    }

                                    Button(
                                        modifier = Modifier.padding(5.dp),
                                        enabled = playEnabled,
                                        onClick = {
                                            stopped = true
                                            track.pause()
                                            track.flush()
                                            onClickPlay()
                                        }) {
                                         Text(stringResource(R.string.btn_play))
                                    }

                                    Button(
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            onClickStop()
                                            startEnabled = true
                                        }) {
                                         Text(stringResource(R.string.btn_stop))
                                    }
                                }

                                Row {
                                    Button(
                                        enabled = saveEnabled,
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            saveLauncher.launch("generated.wav")
                                        }) {
                                         Text(stringResource(R.string.btn_save))
                                    }

                                    Button(
                                        enabled = shareEnabled,
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            val file = File(application.filesDir.absolutePath + "/generated.wav")
                                            if (!file.exists()) {
                                                 Toast.makeText(applicationContext, getString(R.string.toast_no_audio_to_share), Toast.LENGTH_SHORT).show()
                                            } else {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "com.k2fsa.sherpa.onnx.tts.engine.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "audio/wav"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                 startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_audio)))
                                            }
                                        }) {
                                         Text(stringResource(R.string.btn_share))
                                    }
                                }
                                if (rtfText.isNotEmpty()) {
                                    Row {
                                        Text(rtfText)
                                    }
                                }
                                if (detectedLanguagesText.isNotEmpty()) {
                                    Row(modifier = Modifier.padding(top = 10.dp)) {
                                         Text(stringResource(R.string.sub_sentences_label, detectedLanguagesText))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        stopMediaPlayer()
        languageDetector?.close()
        super.onDestroy()
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun onClickPlay() {
        val filename = application.filesDir.absolutePath + "/generated.wav"
        stopMediaPlayer()
        mediaPlayer = MediaPlayer.create(
            applicationContext,
            Uri.fromFile(File(filename))
        )
        mediaPlayer?.start()
    }

    private fun onClickStop() {
        stopped = true
        track.pause()
        track.flush()

        stopMediaPlayer()
    }

    // this function is called from C++
    private fun callback(samples: FloatArray): Int {
        if (!stopped) {
            val processed = activeResampler?.process(samples) ?: samples
            val samplesCopy = processed.copyOf()
            val currentRate = activeSampleRate
            scope.launch {
                Log.i(TAG, "callback called with ${samplesCopy.count()} samples")
                val ok = samplesChannel.trySend(AudioChunk(samplesCopy, currentRate)).isSuccess
                Log.i(TAG, "callback called with $ok")
            }
            return 1
        } else {
            track.stop()
            Log.i(TAG, " return 0")
            return 0
        }
    }

    private fun initAudioTrack() {
        if (::track.isInitialized) {
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing old AudioTrack", e)
            }
        }
        val sampleRate = TtsEngine.tts?.sampleRate() ?: 22050
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        Log.i(TAG, "sampleRate: $sampleRate, buffLength: $bufLength")

        val attr = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        track = AudioTrack(
            attr, format, bufLength, AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.play()
    }
}
