package com.k2fsa.sherpa.onnx.vad.asr

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.R
import com.k2fsa.sherpa.onnx.SettingsActivity
import com.k2fsa.sherpa.onnx.SettingsActivity.Companion.PREFS_NAME
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getOfflineModelConfig
import com.k2fsa.sherpa.onnx.getVadModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.min

// Add necessary imports for MediaProjection
import android.media.projection.MediaProjectionManager
import android.media.AudioAttributes
import android.media.AudioPlaybackCaptureConfiguration
import android.media.projection.MediaProjection
import com.k2fsa.sherpa.onnx.MediaProjectionService
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import java.lang.reflect.Method
import kotlin.math.max


private const val TAG = "sherpa-onnx"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_READ_AUDIO_PERMISSION = 201
private const val REQUEST_SELECT_AUDIO_FILE = 202
// New request codes for system audio
private const val REQUEST_SYSTEM_AUDIO_PERMISSION = 203
// Media projection request code
private const val REQUEST_MEDIA_PROJECTION = 204

class MainActivity : AppCompatActivity() {

    private val mediaProjectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MediaProjectionService.BROADCAST_MEDIA_PROJECTION_READY) {
                val resultCode = intent.getIntExtra(MediaProjectionService.EXTRA_RESULT_CODE, 0)
                val resultData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(MediaProjectionService.EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(MediaProjectionService.EXTRA_RESULT_DATA)
                }
                
                // Get the MediaProjection using the result code and data
                if (resultCode != 0 && resultData != null) {
                    val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                }
                
                // Initialize audio record for system audio with the media projection
                initializeSystemAudioRecording()
             }
         }
     }
     
     private fun initializeSystemAudioRecording() {
         runOnUiThread {
             // Initialize audio record for system audio with the media projection
             val numBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
             Log.i(TAG, "System audio buffer size in milliseconds: ${numBytes * 1000.0f / sampleRateInHz}")

             // Create audio attributes for system audio capture (Android Q+)
             val audioAttributesBuilder = AudioAttributes.Builder()
                 .setUsage(AudioAttributes.USAGE_MEDIA)
                 .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

             // For Android Q+, we can use AudioPlaybackCaptureConfiguration to capture system audio
             val audioAttributes = audioAttributesBuilder.build()

             // AudioPlaybackCaptureConfiguration will be created later when initializing AudioRecord for Android Q+

             try {
                 val audioFormat = android.media.AudioFormat.Builder()
                     .setEncoding(this.audioFormat)
                     .setSampleRate(sampleRateInHz)
                     .setChannelMask(channelConfig)
                     .build()

                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // For Android Q+, use the constructor that accepts AudioPlaybackCaptureConfiguration
                    val projection = mediaProjection
                    if (projection != null) {
                        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
                            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                            .addMatchingUsage(AudioAttributes.USAGE_GAME)
                            .addMatchingUsage(AudioAttributes.USAGE_ASSISTANT)
                            .build()

                        // Use AudioRecord.Builder for better compatibility across API levels
                        val audioRecordBuilder = AudioRecord.Builder()
                            .setAudioFormat(audioFormat)
                            .setBufferSizeInBytes(numBytes * 2)
                            .setAudioPlaybackCaptureConfig(config)

                        systemAudioRecord = audioRecordBuilder.build()
                    } else {
                        Log.e(TAG, "MediaProjection is null when trying to initialize system audio recording")
                        runOnUiThread {
                            Toast.makeText(this, "媒体投影未获取，无法初始化系统音频录制", Toast.LENGTH_LONG).show()
                        }
                        return@runOnUiThread
                    }
                } else {
                     // Fallback for older Android versions (though system audio capture won't work)
                     // Using reflection to access the constructor that accepts AudioAttributes
                     val audioRecordConstructor = AudioRecord::class.java.getConstructor(
                         AudioAttributes::class.java,
                         android.media.AudioFormat::class.java,
                         Integer.TYPE,
                         Integer.TYPE
                     )

                     systemAudioRecord = audioRecordConstructor.newInstance(
                         audioAttributes,
                         audioFormat,
                         numBytes * 2,
                         AudioManager.AUDIO_SESSION_ID_GENERATE
                     )
                 }
                 
                 

                 // Check if the AudioRecord initialized correctly
                 if (systemAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                     Log.e(TAG, "Failed to initialize system audio recorder. This might be because system audio capture requires special permissions or is not supported on this device.")
                     runOnUiThread {
                         Toast.makeText(this, "系统音频录制初始化失败。此功能可能需要特殊权限或设备不支持。", Toast.LENGTH_LONG).show()
                     }
                     systemAudioRecord?.release()
                     systemAudioRecord = null
                     return@runOnUiThread
                 }

                 systemAudioRecord?.startRecording()
                 isSystemAudioRecording = true

                 // Update button text
                 val systemAudioRecordButton = findViewById<Button>(R.id.record_system_audio_button)
                 systemAudioRecordButton.text = "停止录制系统音频"

                 textView.text = ""
                 lastText = ""
                 idx = 0

                 vad.reset()
                 systemAudioRecordingThread = thread(true) {
                     processSystemAudioSamples()
                 }
                 Log.i(TAG, "Started system audio recording")
             } catch (e: Exception) {
                 Log.e(TAG, "Error initializing system audio recording: ${e.message},${e.stackTrace.contentToString()}")
                 runOnUiThread {
                     Toast.makeText(this, "系统音频录制初始化失败: ${e.message},${e.stackTrace.contentToString()}", Toast.LENGTH_LONG).show()
                 }
             }
         }
     }


    private lateinit var recordButton: Button
    private lateinit var selectAudioButton: Button
    private lateinit var textView: TextView

    private lateinit var vad: Vad

    private var audioRecord: AudioRecord? = null
    private var systemAudioRecord: AudioRecord? = null // For system audio recording
    private var recordingThread: Thread? = null
    private var systemAudioRecordingThread: Thread? = null // For system audio recording thread
    private val audioSource = MediaRecorder.AudioSource.MIC
    private val systemAudioSource = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaRecorder.AudioSource.REMOTE_SUBMIX // For capturing system audio
    } else {
        MediaRecorder.AudioSource.MIC // Fallback for older versions (won't capture system audio without root)
    }
    private val sampleRateInHz = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    // Note: We don't use AudioFormat.ENCODING_PCM_FLOAT
    // since the AudioRecord.read(float[]) needs API level >= 23
    // but we are targeting API level >= 21
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val permissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_AUDIO
    )

    // Non-streaming ASR
    private lateinit var offlineRecognizer: OfflineRecognizer

    private var idx: Int = 0
    private var lastText: String = ""

    @Volatile
    private var isRecording: Boolean = false
    @Volatile
    private var isSystemAudioRecording: Boolean = false // Flag for system audio recording
    // Media projection variables
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: android.media.projection.MediaProjection? = null

    private lateinit var sharedPreferences: SharedPreferences



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        Log.i(TAG, "Start to initialize model")
        initVadModel()
        Log.i(TAG, "Finished initializing model")

        Log.i(TAG, "Start to initialize non-streaimng recognizer")
        initOfflineRecognizerAsync()
        Log.i(TAG, "Initializing non-streaming recognizer in background")

        recordButton = findViewById(R.id.record_button)
        recordButton.setOnClickListener { onclick() }

        selectAudioButton = findViewById(R.id.select_audio_button)
        selectAudioButton.setOnClickListener { onSelectAudioClick() }

        val settingsButton = findViewById<Button>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Add system audio recording button
        val systemAudioRecordButton = findViewById<Button>(R.id.record_system_audio_button)
        systemAudioRecordButton.setOnClickListener { onSystemAudioClick() }

        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()
        
        // Initialize MediaProjectionManager for system audio capture
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun onSystemAudioClick() {
        if (!isSystemAudioRecording) {
            startSystemAudioRecording()
        } else {
            stopSystemAudioRecording()
        }
    }

    private fun startSystemAudioRecording() {
        if (isSystemAudioRecording) return

        // Check if we have audio recording permission
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        // For Android Q and above, we need to use MediaProjection to capture system audio
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Start media projection intent to get permission to capture system audio
            val intent = mediaProjectionManager?.createScreenCaptureIntent()
            intent?.let {
                startActivityForResult(it, REQUEST_MEDIA_PROJECTION)
            } ?: run {
                Log.e(TAG, "Failed to create media projection intent")
                runOnUiThread {
                    Toast.makeText(this, "无法创建媒体投影意图", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // For older versions, we cannot capture system audio without special permissions or root
            runOnUiThread {
                Toast.makeText(this, "系统音频录制需要Android 10或更高版本", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode == RESULT_OK && data != null) {
                    // Register the broadcast receiver to listen for media projection ready
                    LocalBroadcastManager.getInstance(this).registerReceiver(
                        mediaProjectionReceiver,
                        IntentFilter(MediaProjectionService.BROADCAST_MEDIA_PROJECTION_READY)
                    )

                    // Start the foreground service before getting media projection
                    val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
                        action = MediaProjectionService.ACTION_START
                        putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode)
                        putExtra(MediaProjectionService.EXTRA_RESULT_DATA, data)
                    }
                    startForegroundService(serviceIntent)
                } else {
                    Log.e(TAG, "媒体投影被拒绝或失败")
                    runOnUiThread {
                        Toast.makeText(this, "媒体投影被拒绝或失败", Toast.LENGTH_LONG).show()
                    }
                }
            }

            REQUEST_SELECT_AUDIO_FILE -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { uri ->
                        processSelectedAudio(uri)
                    }
                }
            }
        }
    }

    private fun stopSystemAudioRecording() {
        if (!isSystemAudioRecording) return

        isSystemAudioRecording = false

        systemAudioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        systemAudioRecord = null

        // Release the media projection
        mediaProjection?.stop()
        mediaProjection = null

        systemAudioRecordingThread?.interrupt()
        systemAudioRecordingThread = null

        // Stop the foreground service
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        stopService(serviceIntent)

        // Update button text
        val systemAudioRecordButton = findViewById<Button>(R.id.record_system_audio_button)
        systemAudioRecordButton.text = "录制系统音频"

        Log.i(TAG, "Stopped system audio recording")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Make sure to stop the service and release media projection when activity is destroyed
        if (mediaProjection != null) {
            mediaProjection?.stop()
            mediaProjection = null
        }
        
        val serviceIntent = Intent(this, MediaProjectionService::class.java)
        stopService(serviceIntent)
    }

    private fun processSystemAudioSamples() {
        Log.i(TAG, "processing system audio samples")

        val bufferSize = 512 // in samples
        val buffer = ShortArray(bufferSize)
        val coroutineScope = CoroutineScope(Dispatchers.IO)


        while (isSystemAudioRecording) {
            val ret = systemAudioRecord?.read(buffer, 0, buffer.size)
            if (ret != null && ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }

                vad.acceptWaveform(samples)
                while(!vad.empty()) {
                    var segment = vad.front()
                    coroutineScope.launch {
                        val text = runSecondPass(segment.samples)
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                lastText = "${lastText}\n${idx}: ${text}"
                                idx += 1
                                textView.text = lastText.lowercase()
                            }
                        }
                    }

                    vad.pop();
                }

                val isSpeechDetected = vad.isSpeechDetected()

                runOnUiThread {
                    textView.text = lastText.lowercase()
                }
            }
        }

        // Clean up the coroutine scope when done
        coroutineScope.cancel()
    }

    private fun onclick() {
        if (!isRecording) {
            val ret = initMicrophone()
            if (!ret) {
                Log.e(TAG, "Failed to initialize microphone")
                return
            }
            Log.i(TAG, "state: ${audioRecord?.state}")
            audioRecord!!.startRecording()
            recordButton.setText(R.string.stop)
            isRecording = true

            textView.text = ""
            lastText = ""
            idx = 0

            vad.reset()
            recordingThread = thread(true) {
                processSamples()
            }
            Log.i(TAG, "Started recording")
        } else {
            isRecording = false

            audioRecord!!.stop()
            audioRecord!!.release()
            audioRecord = null

            recordButton.setText(R.string.start)
            Log.i(TAG, "Stopped recording")
        }
    }

    private fun onSelectAudioClick() {
        // Check permission for reading audio files
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_READ_AUDIO_PERMISSION
            )
        } else {
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            // 支持音频和视频文件
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_SELECT_AUDIO_FILE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                val permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (!permissionToRecordAccepted) {
                    Log.e(TAG, "Audio record is disallowed")
                    finish()
                }
                Log.i(TAG, "Audio record is permitted")
            }
            REQUEST_READ_AUDIO_PERMISSION -> {
                val readAudioPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (readAudioPermissionGranted) {
                    openFilePicker()
                } else {
                    Log.e(TAG, "Read audio permission is disallowed")
                }
            }
        }
    }



    private fun processSelectedAudio(uri: Uri) {
        // Show processing message
        runOnUiThread {
            textView.append("\n正在处理文件...")
        }

        // Get the original file path
        val originalFilePath = getRealPathFromURI(uri)

        // 检查文件扩展名以确定是否为视频文件
        val fileExtension = originalFilePath.substringAfterLast(".", "").lowercase()
        val isVideoFile = listOf("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v", "3gp", "3gpp").contains(fileExtension)

        if (isVideoFile) {
            // 如果是视频文件，先提取音频
            extractAudioFromVideo(originalFilePath)
        } else {
            // 如果是音频文件，直接转换为16kHz WAV
            convertAudioToWav(originalFilePath)
        }
    }

    private fun extractAudioFromVideo(videoPath: String) {
        runOnUiThread {
            textView.append("\n正在从视频中提取音频...")
        }

        // Create temporary output file for extracted audio
        val extractedAudioFile = File(filesDir, "extracted_audio_${System.currentTimeMillis().toString()}.wav")
        val extractedAudioPath = extractedAudioFile.absolutePath

        // Use FFmpeg to extract audio from video
        val ffmpegCommand = "-i \"${videoPath}\" -vn -acodec pcm_s16le -ar 16000 -ac 1 \"${extractedAudioPath}\""

        FFmpegKit.executeAsync(ffmpegCommand) { session ->
            val returnCode = session.getReturnCode()

            if (ReturnCode.isSuccess(returnCode)) {
                Log.i(TAG, "Audio extracted successfully from video")
                // Process the extracted audio file
                runOnUiThread {
                    textView.append("\n音频提取完成，正在转换格式...")
                }
                // 现在对提取的音频进行16kHz转换（虽然上面已经设置了16kHz，但再次确认格式）
                convertAudioToWav(extractedAudioPath)
            } else {
                Log.e(TAG, "FFmpeg command failed: ${session.getFailStackTrace()}")
                runOnUiThread {
                    textView.append("\n视频转音频失败: ${session.getFailStackTrace()}")
                }
            }
        }
    }

    private fun convertAudioToWav(audioPath: String) {
        // Create temporary output file for converted audio
        val outputAudioFile = File(filesDir, "converted_audio_${System.currentTimeMillis().toString()}.wav")
        val outputAudioPath = outputAudioFile.absolutePath

        // Use FFmpeg to convert the audio to 16kHz WAV if needed
        val ffmpegCommand = "-i \"${audioPath}\" -ar 16000 -ac 1 -c:a pcm_s16le \"${outputAudioPath}\""

        FFmpegKit.executeAsync(ffmpegCommand) { session ->
            val returnCode = session.getReturnCode()

            if (ReturnCode.isSuccess(returnCode)) {
                Log.i(TAG, "Audio converted successfully to 16kHz WAV")
                // Process the converted audio file
                runOnUiThread {
                    textView.append("\n音频转换完成，正在识别...")
                }
                recognizeAudioFile(outputAudioPath)
            } else {
                Log.e(TAG, "FFmpeg command failed: ${session.getFailStackTrace()}")
                runOnUiThread {
                    textView.append("\n音频转换失败: ${session.getFailStackTrace()}")
                }
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        // For Android 10+, we need to copy the file to app's private storage
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val inputStream = contentResolver.openInputStream(uri)
            val outputFile = File(filesDir, "temp_audio_file")
            val outputStream = FileOutputStream(outputFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            return outputFile.absolutePath
        } else {
            // For older versions, try to get the real path
            val projection = arrayOf(android.provider.MediaStore.Audio.Media.DATA)
            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA)
                    return it.getString(columnIndex)
                }
            }
            return uri.path ?: ""
        }
    }

    /**
     * 将采样点索引转换为SRT字幕格式的时间戳
     * @param sampleIndex 采样点在音频中的索引
     * @param sampleRate 采样率（默认16000）
     * @return 格式化的时间戳字符串 (HH:MM:SS,mmm)
     */
    private fun sampleIndexToSrtTimestamp(sampleIndex: Int, sampleRate: Int = 16000): String {
        val totalSeconds = sampleIndex.toDouble() / sampleRate
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60)
        val milliseconds = ((seconds - seconds.toInt()) * 1000).toInt()
        
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds.toInt(), milliseconds)
    }

    private fun recognizeAudioFile(audioFilePath: String) {
        Thread {
            try {
                // 记录开始时间
                val startTime = System.currentTimeMillis()

                // Read the converted WAV file
                val audioFile = File(audioFilePath)
                if (!audioFile.exists()) {
                    Log.e(TAG, "Audio file does not exist: $audioFile")
                    return@Thread
                }

                // Decode the WAV file to get the audio samples
                val samples = decodeWavFile(audioFile)

                // Use VAD to split the audio into segments first
                vad.reset()

                // Process the samples in chunks, similar to how real-time recording works
                val chunkSize = 512 // Use the same buffer size as in processSamples
                var startIndex = 0

                // 收集所有VAD检测到的语音段
                val allSegments = mutableListOf<SegmentWithIndex>()

                while (startIndex < samples.size) {
                    val endIndex = kotlin.math.min(startIndex + chunkSize, samples.size)
                    val chunk = samples.copyOfRange(startIndex, endIndex)

                    vad.acceptWaveform(chunk)

                    // 收集所有可用的语音段
                    while (!vad.empty()) {
                        val segment = vad.front()
                        // 按要求处理音频段：先添加0.09秒实际音频内容，再添加0.3秒静音
                        val paddedSamples = addPaddingToSegmentWithAudioAndSilence(segment.samples, segment.start, samples)
                        
                        // 保存语音段及其原始索引位置
                        allSegments.add(SegmentWithIndex(segment.start, paddedSamples))
                        vad.pop()
                    }

                    startIndex = endIndex
                }

                // Flush any remaining samples
                vad.flush()

                // 收集剩余的语音段
                while (!vad.empty()) {
                    val segment = vad.front()
                    val paddedSamples = addPaddingToSegmentWithAudioAndSilence(segment.samples, segment.start, samples)
                    allSegments.add(SegmentWithIndex(segment.start, paddedSamples))
                    vad.pop()
                }

                // 计算总段数
                val totalSegments = allSegments.size
                runOnUiThread {
                    textView.append("\n共检测到 $totalSegments 个语音段，正在并行转录...")
                }

                // 使用协程并行处理所有语音段
                val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
                val deferredResults = mutableListOf<Deferred<TranscriptionResult?>>()

                for (segment in allSegments) {
                    val deferred = coroutineScope.async {
                        try {
                            val text = runSecondPass(segment.paddedSamples)
                            if (text.isNotBlank()) {
                                // 格式化文本
                                val formattedText = if (text.endsWith('.') || text.endsWith('。') || text.endsWith('!') || text.endsWith('?') || text.endsWith('！') || text.endsWith('？')) {
                                    text
                                } else {
                                    "$text。"
                                }
                                
                                // 计算该段的时间戳
                                val segmentEndIndex = segment.start + (segment.paddedSamples.size - (sampleRateInHz * 0.39f).toInt()) // 减去添加的padding
                                val startTimeStamp = sampleIndexToSrtTimestamp(segment.start)
                                val endTimeStamp = sampleIndexToSrtTimestamp(segmentEndIndex)
                                
                                TranscriptionResult(segment.start, formattedText, startTimeStamp, endTimeStamp)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error transcribing segment at ${segment.start}: ${e.message}", e)
                            null
                        }
                    }
                    deferredResults.add(deferred)
                }

                // 等待所有转录任务完成并收集结果
                val results = runBlocking { deferredResults.awaitAll() }
                
                // 按原始顺序排序结果（尽管应该已经是顺序的，但保险起见）
                val sortedResults = results.filterNotNull().sortedBy { it.startIndex }

                // 输出结果
                sortedResults.forEachIndexed { index, result ->
                    // 计算进度百分比和耗时
                    val progress = kotlin.math.min(((index + 1).toDouble() / totalSegments * 100).toInt(), 100)
                    val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                    val formattedOutput = String.format("[时间戳: %s --> %s, 进度: %d%%, 耗时: %.1f秒] %s", 
                        result.startTimeStamp, result.endTimeStamp, progress, elapsedTime, result.text)
                    
                    runOnUiThread {
                        lastText = "${lastText}\n音频文件识别结果: $formattedOutput"
                        idx += 1
                        textView.append("\n音频文件识别结果: $formattedOutput")
                    }
                }

                // 清理协程作用域
                coroutineScope.cancel()

                // 最终完成信息
                val finalElapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                runOnUiThread {
                    textView.append("\n音频文件识别完成，总耗时: %.1f秒".format(finalElapsedTime))
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing audio file: ${e.message}", e)
                runOnUiThread {
                    textView.append("\n处理音频文件时出错: ${e.message}")
                }
            }
        }.start()
    }

    // 辅助类：存储带索引的语音段
    private data class SegmentWithIndex(val start: Int, val paddedSamples: FloatArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SegmentWithIndex
            if (start != other.start) return false
            if (!paddedSamples.contentEquals(other.paddedSamples)) return false
            return true
        }
        override fun hashCode(): Int {
            var result = start
            result = 31 * result + paddedSamples.contentHashCode()
            return result
        }
    }

    // 辅助类：存储转录结果
    private data class TranscriptionResult(
        val startIndex: Int,
        val text: String,
        val startTimeStamp: String,
        val endTimeStamp: String
    )

    private fun decodeWavFile(wavFile: File): FloatArray {
        // Read WAV file header and extract audio samples
        val fileBytes = wavFile.readBytes()

        // WAV file format: RIFF header (12 bytes) + fmt chunk (24 bytes) + data chunk header (8 bytes) + audio data
        // Skip the RIFF header (12 bytes)
        var offset = 12

        // Skip fmt chunk (usually 24 bytes total: "fmt " + 4 + 16 + format data)
        val fmtChunkSize = ((fileBytes[offset + 7].toInt() and 0xFF) shl 24) or
                ((fileBytes[offset + 6].toInt() and 0xFF) shl 16) or
                ((fileBytes[offset + 5].toInt() and 0xFF) shl 8) or
                (fileBytes[offset + 4].toInt() and 0xFF)
        offset += 8 + fmtChunkSize // Skip fmt chunk header + data

        // Find data chunk
        while (offset < fileBytes.size - 4) {
            val chunkId = String(
                byteArrayOf(
                    fileBytes[offset],
                    fileBytes[offset + 1],
                    fileBytes[offset + 2],
                    fileBytes[offset + 3]
                )
            )

            if (chunkId == "data") {
                offset += 4 // Skip "data" id
                val dataSize = ((fileBytes[offset + 3].toInt() and 0xFF) shl 24) or
                        ((fileBytes[offset + 2].toInt() and 0xFF) shl 16) or
                        ((fileBytes[offset + 1].toInt() and 0xFF) shl 8) or
                        (fileBytes[offset].toInt() and 0xFF)
                offset += 4 // Skip data size

                // Read audio samples (assuming 16-bit PCM)
                val numSamples = dataSize / 2
                val samples = FloatArray(numSamples)

                for (i in 0 until numSamples) {
                    val sample = (fileBytes[offset + i * 2 + 1].toInt() shl 8) or
                            (fileBytes[offset + i * 2].toInt() and 0xFF)
                    // Convert to signed 16-bit and then to float
                    val signedSample = if (sample > 32767) sample - 65536 else sample
                    samples[i] = signedSample / 32768.0f
                }

                return samples
            } else {
                // Skip this chunk
                val chunkSize = ((fileBytes[offset + 7].toInt() and 0xFF) shl 24) or
                        ((fileBytes[offset + 6].toInt() and 0xFF) shl 16) or
                        ((fileBytes[offset + 5].toInt() and 0xFF) shl 8) or
                        (fileBytes[offset + 4].toInt() and 0xFF)
                offset += 8 + chunkSize
            }
        }

        throw Exception("Could not find data chunk in WAV file")
    }

    private  fun initVadModel() {
        val type = 0
        Log.i(TAG, "Select VAD model type ${type}")
        val config = getVadModelConfig(type)

        vad = Vad(
            assetManager = application.assets,
            config = config!!,
        )
    }

    private fun initMicrophone(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
            return false
        }

        val numBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        Log.i(
            TAG, "buffer size in milliseconds: ${numBytes * 1000.0f / sampleRateInHz}"
        )

        audioRecord = AudioRecord(
            audioSource,
            sampleRateInHz,
            channelConfig,
            audioFormat,
            numBytes * 2 // a sample has two bytes as we are using 16-bit PCM
        )
        return true
    }

    private fun processSamples() {
        Log.i(TAG, "processing samples")

        val bufferSize = 512 // in samples
        val buffer = ShortArray(bufferSize)
        val coroutineScope = CoroutineScope(Dispatchers.IO)


        while (isRecording) {
            val ret = audioRecord?.read(buffer, 0, buffer.size)
            if (ret != null && ret > 0) {
                val samples = FloatArray(ret) { buffer[it] / 32768.0f }

                vad.acceptWaveform(samples)
                while(!vad.empty()) {
                    var segment = vad.front()
                    coroutineScope.launch {
                        val text = runSecondPass(segment.samples)
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                lastText = "${lastText}\n${idx}: ${text}"
                                idx += 1
                                textView.text = lastText.lowercase()
                            }
                        }
                    }

                    vad.pop();
                }

                val isSpeechDetected = vad.isSpeechDetected()

                runOnUiThread {
                    textView.text = lastText.lowercase()
                }
            }
        }

        // Clean up the coroutine scope when done
        coroutineScope.cancel()
    }

    private fun initOfflineRecognizerAsync() {
        Thread {
            val startTime = System.currentTimeMillis() // 记录开始时间
            try {
                // Get model type and language from SharedPreferences
                val asrModelType = sharedPreferences.getInt("model_type", 24)
                val selectedLanguage = sharedPreferences.getString("whisper_language", "") ?: ""
                val asrRuleFsts: String?
                asrRuleFsts = null
                Log.i(TAG, "Select model type ${asrModelType} for ASR")

                val config = OfflineRecognizerConfig(
                    featConfig = getFeatureConfig(sampleRate = sampleRateInHz, featureDim = 80),
                    modelConfig = getOfflineModelConfig(type = asrModelType)!!.apply {
                        if (asrModelType == 3) { // Whisper model
                            whisper.language = selectedLanguage
                        }
                    },
                )
                if (asrRuleFsts != null) {
                    config.ruleFsts = asrRuleFsts;
                }

                offlineRecognizer = OfflineRecognizer(
                    assetManager = application.assets,
                    config = config,
                )

                val endTime = System.currentTimeMillis() // 记录结束时间
                val duration = endTime - startTime // 计算耗时

                // 在主线程显示加载成功的Toast，包含耗时信息
                runOnUiThread {
                    textView.append("\nASR模型加载成功 (耗时: ${duration}ms)，语言：${selectedLanguage}，模型类型：${asrModelType}")
                    Toast.makeText(this, "ASR模型加载成功 (耗时: ${duration}ms)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val endTime = System.currentTimeMillis() // 记录结束时间
                val duration = endTime - startTime // 计算耗时
                Log.e(TAG, "Error initializing offline recognizer: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "ASR模型加载失败 (耗时: ${duration}ms): ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun runSecondPass(samples: FloatArray): String {
        val stream = offlineRecognizer.createStream()
        stream.acceptWaveform(samples, sampleRateInHz)
        offlineRecognizer.decode(stream)
        val result = offlineRecognizer.getResult(stream)
        stream.release()
        return result.text
    }

    /**
     * 为语音段添加首尾padding，避免句子首尾丢字问题
     * @param samples 原始语音样本
     * @param paddingDurationSeconds 需要添加的padding时长（秒）
     * @return 添加padding后的语音样本
     */
    private fun addPaddingToSegment(samples: FloatArray, paddingDurationSeconds: Float = 0.5F): FloatArray {
        val paddingSamplesCount = (sampleRateInHz * paddingDurationSeconds).toInt()
        val paddedSamples = FloatArray(samples.size + paddingSamplesCount * 2) // 前后各添加padding

        // 填充开头的静音（0值）
        for (i in 0 until paddingSamplesCount) {
            paddedSamples[i] = 0.0f
        }

        // 复制原始样本到中间部分
        System.arraycopy(samples, 0, paddedSamples, paddingSamplesCount, samples.size)

        // 填充末尾的静音（0值）
        for (i in (samples.size + paddingSamplesCount) until paddedSamples.size) {
            paddedSamples[i] = 0.0f
        }

        return paddedSamples
    }

    /**
     * 为语音段添加首尾padding，按要求：先添加0.09秒实际音频内容，再添加0.3秒静音
     * @param segmentSamples 被VAD分割的音频段样本
     * @param segmentStartIndex 音频段在原音频中的起始索引
     * @param originalSamples 原始完整音频样本
     * @return 添加padding后的语音样本
     */
    private fun addPaddingToSegmentWithAudioAndSilence(
        segmentSamples: FloatArray,
        segmentStartIndex: Int,
        originalSamples: FloatArray
    ): FloatArray {
        // 计算0.09秒和0.3秒对应的采样点数
        val audioPaddingSamplesCount = (sampleRateInHz * 0.09f).toInt()
        val silencePaddingSamplesCount = (sampleRateInHz * 0.3f).toInt()

        // 计算最终数组大小：原始段 + 前后音频padding + 前后静音padding
        val totalPaddingSize = (audioPaddingSamplesCount + silencePaddingSamplesCount) * 2
        val paddedSamples = FloatArray(segmentSamples.size + totalPaddingSize)

        var currentIndex = 0

        // 1. 添加头部0.09秒实际音频内容
        val startAudioPaddingStartIndex = maxOf(0, segmentStartIndex - audioPaddingSamplesCount)
        val actualAudioPaddingSize = minOf(audioPaddingSamplesCount, segmentStartIndex)
        if (actualAudioPaddingSize > 0) {
            System.arraycopy(
                originalSamples,
                startAudioPaddingStartIndex,
                paddedSamples,
                currentIndex,
                actualAudioPaddingSize
            )
            currentIndex += actualAudioPaddingSize
        }

        // 如果实际音频不够0.09秒，则用静音填充剩余部分
        if (actualAudioPaddingSize < audioPaddingSamplesCount) {
            val silenceFillCount = audioPaddingSamplesCount - actualAudioPaddingSize
            for (i in 0 until silenceFillCount) {
                paddedSamples[currentIndex + i] = 0.0f
            }
            currentIndex += silenceFillCount
        }

        // 2. 添加头部0.3秒静音
        for (i in 0 until silencePaddingSamplesCount) {
            paddedSamples[currentIndex + i] = 0.0f
        }
        currentIndex += silencePaddingSamplesCount

        // 3. 复制原始语音段
        System.arraycopy(segmentSamples, 0, paddedSamples, currentIndex, segmentSamples.size)
        currentIndex += segmentSamples.size

        // 4. 添加尾部0.3秒静音
        for (i in 0 until silencePaddingSamplesCount) {
            paddedSamples[currentIndex + i] = 0.0f
        }
        currentIndex += silencePaddingSamplesCount

        // 5. 添加尾部0.09秒实际音频内容
        val segmentEndIndex = segmentStartIndex + segmentSamples.size
        val endAudioPaddingStartIndex = minOf(originalSamples.size - 1, segmentEndIndex)
        val endAudioPaddingEndIndex = minOf(originalSamples.size, segmentEndIndex + audioPaddingSamplesCount)
        val endAudioActualSize = endAudioPaddingEndIndex - endAudioPaddingStartIndex

        if (endAudioActualSize > 0) {
            System.arraycopy(
                originalSamples,
                endAudioPaddingStartIndex,
                paddedSamples,
                currentIndex,
                endAudioActualSize
            )
            currentIndex += endAudioActualSize

            // 如果实际音频不够0.09秒，则用静音填充剩余部分
            if (endAudioActualSize < audioPaddingSamplesCount) {
                val silenceFillCount = audioPaddingSamplesCount - endAudioActualSize
                for (i in 0 until silenceFillCount) {
                    paddedSamples[currentIndex + i] = 0.0f
                }
            }
        }

        return paddedSamples
    }
}