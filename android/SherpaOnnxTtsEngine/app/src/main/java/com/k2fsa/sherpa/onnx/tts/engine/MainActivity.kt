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
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import com.github.pemistahl.lingua.api.Language

const val TAG = "sherpa-onnx-tts-engine"

class MainActivity : ComponentActivity() {
    private val languageDetector by lazy {
        LanguageDetectorBuilder.fromAllLanguages().build()
    }
    // TODO(fangjun): Save settings in ttsViewModel
    private val ttsViewModel: TtsViewModel by viewModels()

    private var mediaPlayer: MediaPlayer? = null

    // see
    // https://developer.android.com/reference/kotlin/android/media/AudioTrack
    private lateinit var track: AudioTrack

    private var stopped: Boolean = false

    private var samplesChannel = Channel<FloatArray>(capacity = 128)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "Start to initialize TTS")
        TtsEngine.createTts(this)
        Log.i(TAG, "Finish initializing TTS")

        Log.i(TAG, "Start to initialize AudioTrack")
        initAudioTrack()
        Log.i(TAG, "Finish initializing AudioTrack")

        val preferenceHelper = PreferenceHelper(this)
        setContent {
            SherpaOnnxTtsEngineTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(topBar = {
                        TopAppBar(title = { Text("Next-gen Kaldi: TTS Engine") })
                    }) {
                        Box(modifier = Modifier.padding(it)) {
                            val context = LocalContext.current
                            Column(modifier = Modifier.padding(16.dp)) {
                                var expandedModel by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(
                                    expanded = expandedModel,
                                    onExpandedChange = { expandedModel = !expandedModel },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    OutlinedTextField(
                                        value = TtsEngine.currentModel.name,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Model") },
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
                                                        TtsEngine.updateTts(context)
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Model '${model.name}' is missing required files and cannot be loaded.",
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
                                    Text("Speed " + String.format("%.1f", TtsEngine.speed))
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
                                        expanded = expanded,
                                        onExpandedChange = { expanded = !expanded },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = Languages.getName(TtsEngine.supertonicLang),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Language") },
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

                                val testTextContent = getSampleText(
                                    if (TtsEngine.isSupertonic) Languages.getIso3Code(
                                        TtsEngine.supertonicLang
                                    ) else (TtsEngine.lang ?: "")
                                )

                                var testText by remember { mutableStateOf("") }

                                // Update testText when the language or model changes
                                androidx.compose.runtime.LaunchedEffect(
                                    TtsEngine.supertonicLangState.value,
                                    TtsEngine.modelState.value
                                ) {
                                    testText = getSampleText(
                                        if (TtsEngine.isSupertonic) Languages.getIso3Code(
                                            TtsEngine.supertonicLang
                                        ) else (TtsEngine.lang ?: "")
                                    )
                                }
                                var startEnabled by remember { mutableStateOf(true) }
                                var playEnabled by remember { mutableStateOf(false) }
                                var saveEnabled by remember { mutableStateOf(false) }
                                var shareEnabled by remember { mutableStateOf(false) }
                                var rtfText by remember {
                                    mutableStateOf("")
                                }
                                val scrollState = rememberScrollState(0)

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
                                            Toast.makeText(applicationContext, "Audio saved", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to save audio: $e")
                                            Toast.makeText(applicationContext, "Failed to save audio", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }

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
                                            Text("Speaker ID: (0-${numSpeakers - 1})")
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .wrapContentHeight(),
                                    )
                                }

                                OutlinedTextField(
                                    value = testText,
                                    onValueChange = { testText = it },
                                    label = { Text("Please input your text here") },
                                    maxLines = 10,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .verticalScroll(scrollState)
                                        .wrapContentHeight(),
                                    singleLine = false,
                                )

                                Row {
                                    Button(
                                        enabled = startEnabled,
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            Log.i(TAG, "Clicked, text: $testText")
                                            if (testText.isBlank() || testText.isEmpty()) {
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Please input some text to generate",
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
                                                Log.i(TAG, "Started with text $testText")

                                                scope.launch {
                                                    for (samples in samplesChannel) {
                                                        if (samples.isEmpty()) {
                                                            break
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

                                                    if (TtsEngine.isSupertonic) {
                                                        val iterator = BreakIterator.getSentenceInstance(Locale.getDefault())
                                                        iterator.setText(testText)
                                                        val sentences = mutableListOf<String>()
                                                        var start = iterator.first()
                                                        var end = iterator.next()
                                                        while (end != BreakIterator.DONE) {
                                                            val sentence = testText.substring(start, end).trim()
                                                            if (sentence.isNotEmpty()) {
                                                                sentences.add(sentence)
                                                            }
                                                            start = end
                                                            end = iterator.next()
                                                        }
                                                        if (sentences.isEmpty() && testText.isNotBlank()) {
                                                            sentences.add(testText)
                                                        }

                                                        for (sentence in sentences) {
                                                            if (stopped) break
                                                            val detected = languageDetector.detectLanguageOf(sentence)
                                                            val iso1 = Languages.mapLinguaToIso1(detected) ?: TtsEngine.supertonicLang
                                                            Log.i(TAG, "Sentence: '$sentence', detected language: $detected, iso1: $iso1")

                                                            val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = TtsEngine.speed)
                                                            genConfig.extra = mapOf("lang" to iso1)

                                                            val audio = TtsEngine.tts!!.generateWithConfigAndCallback(
                                                                text = sentence,
                                                                config = genConfig,
                                                                callback = ::callback,
                                                            )
                                                            allSamples.add(audio.samples)
                                                        }
                                                    } else {
                                                        val genConfig = GenerationConfig(sid = TtsEngine.speakerId, speed = TtsEngine.speed)
                                                        val audio =
                                                            TtsEngine.tts!!.generateWithConfigAndCallback(
                                                                text = testText,
                                                                config = genConfig,
                                                                callback = ::callback,
                                                            )
                                                        allSamples.add(audio.samples)
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
                                                    val RTF = String.format(
                                                        "Number of threads: %d\nElapsed: %.3f s\nAudio duration: %.3f s\nRTF: %.3f/%.3f = %.3f",
                                                        TtsEngine.tts!!.config.model.numThreads,
                                                        elapsed,
                                                        audioDuration,
                                                        elapsed,
                                                        audioDuration,
                                                        if (audioDuration > 0) elapsed / audioDuration else 0f
                                                    )

                                                    scope.launch {
                                                        Log.i(TAG, "send 0 samples")
                                                            samplesChannel.send(FloatArray(0))
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
                                        Text("Start")
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
                                        Text("Play")
                                    }

                                    Button(
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            onClickStop()
                                            startEnabled = true
                                        }) {
                                        Text("Stop")
                                    }
                                }

                                Row {
                                    Button(
                                        enabled = saveEnabled,
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            saveLauncher.launch("generated.wav")
                                        }) {
                                        Text("Save")
                                    }

                                    Button(
                                        enabled = shareEnabled,
                                        modifier = Modifier.padding(5.dp),
                                        onClick = {
                                            val file = File(application.filesDir.absolutePath + "/generated.wav")
                                            if (!file.exists()) {
                                                Toast.makeText(applicationContext, "No audio to share", Toast.LENGTH_SHORT).show()
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
                                                startActivity(Intent.createChooser(intent, "Share audio"))
                                            }
                                        }) {
                                        Text("Share")
                                    }
                                }
                                if (rtfText.isNotEmpty()) {
                                    Row {
                                        Text(rtfText)
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
            val samplesCopy = samples.copyOf()
            scope.launch {
                Log.i(TAG, "callback called with ${samplesCopy.count()} samples")
                val ok = samplesChannel.trySend(samplesCopy).isSuccess
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
        val sampleRate = TtsEngine.tts!!.sampleRate()
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
