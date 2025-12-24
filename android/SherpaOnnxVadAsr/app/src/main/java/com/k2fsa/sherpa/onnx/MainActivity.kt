package com.k2fsa.sherpa.onnx.vad.asr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.R
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.getFeatureConfig
import com.k2fsa.sherpa.onnx.getOfflineModelConfig
import com.k2fsa.sherpa.onnx.getVadModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.math.min


private const val TAG = "sherpa-onnx"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_READ_AUDIO_PERMISSION = 201
private const val REQUEST_SELECT_AUDIO_FILE = 202

class MainActivity : AppCompatActivity() {

    private lateinit var recordButton: Button
    private lateinit var selectAudioButton: Button
    private lateinit var textView: TextView

    private lateinit var vad: Vad

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val audioSource = MediaRecorder.AudioSource.MIC
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        Log.i(TAG, "Start to initialize model")
        initVadModel()
        Log.i(TAG, "Finished initializing model")

        Log.i(TAG, "Start to initialize non-streaimng recognizer")
        initOfflineRecognizer()
        Log.i(TAG, "Finished initializing non-streaming recognizer")

        recordButton = findViewById(R.id.record_button)
        recordButton.setOnClickListener { onclick() }

        selectAudioButton = findViewById(R.id.select_audio_button)
        selectAudioButton.setOnClickListener { onSelectAudioClick() }

        textView = findViewById(R.id.my_text)
        textView.movementMethod = ScrollingMovementMethod()
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
            type = "audio/*"
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_AUDIO_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                processSelectedAudio(uri)
            }
        }
    }

    private fun processSelectedAudio(uri: Uri) {
        // Show processing message
        runOnUiThread {
            textView.append("\n正在处理音频文件...")
        }

        // Get the original audio file path
        val originalAudioPath = getRealPathFromURI(uri)

        // Create temporary output file for converted audio
        val outputAudioFile = File(filesDir, "converted_audio_${System.currentTimeMillis().toString()}.wav")
        val outputAudioPath = outputAudioFile.absolutePath

        // Use FFmpeg to convert the audio to 16kHz WAV
        val ffmpegCommand = "-i \"${originalAudioPath}\" -ar 16000 -ac 1 -c:a pcm_s16le \"${outputAudioPath}\""

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

                // Use VAD to split the audio into segments and process each segment
                vad.reset()
                
                // Process the samples in chunks, similar to how real-time recording works
                val chunkSize = 512 // Use the same buffer size as in processSamples
                var startIndex = 0
                
                // 计算总块数用于进度计算
                val totalChunks = kotlin.math.ceil(samples.size.toDouble() / chunkSize).toInt()
                var processedChunks = 0
                
                while (startIndex < samples.size) {
                    val endIndex = kotlin.math.min(startIndex + chunkSize, samples.size)
                    val chunk = samples.copyOfRange(startIndex, endIndex)
                    
                    vad.acceptWaveform(chunk)
                    
                    // Process any available segments immediately
                    while (!vad.empty()) {
                        val segment = vad.front()
                        // 为语音段添加末尾padding，避免句子末尾丢字问题
                        val paddedSamples = addPaddingToSegment(segment.samples)
                        val text = runSecondPass(paddedSamples)
                        if (text.isNotBlank()) {
                            // Add a period to the end of the text if it doesn't already have one
                            val formattedText = if (text.endsWith('.') || text.endsWith('。') || text.endsWith('!') || text.endsWith('?') || text.endsWith('！') || text.endsWith('？')) {
                                text
                            } else {
                                "$text。"
                            }
                            // 计算进度百分比和耗时
                            val progress = kotlin.math.min(((processedChunks.toDouble() / totalChunks) * 100).toInt(), 100)
                            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                            val formattedOutput = String.format("[进度: %d%%, 耗时: %.1f秒] %s", progress, elapsedTime, formattedText)
                            runOnUiThread {
                                lastText = "${lastText}\n音频文件识别结果: $formattedOutput"
                                idx += 1
                                textView.append("\n音频文件识别结果: $formattedOutput")
                            }
                        }
                        vad.pop()
                    }
                    
                    startIndex = endIndex
                    processedChunks++
                }
                
                // Flush any remaining samples
                vad.flush()
                
                // Process any remaining segments after flushing
                while (!vad.empty()) {
                    val segment = vad.front()
                    // 为语音段添加末尾padding，避免句子末尾丢字问题
                    val paddedSamples = addPaddingToSegment(segment.samples)
                    val text = runSecondPass(paddedSamples)
                    if (text.isNotBlank()) {
                        // Add a period to the end of the text if it doesn't already have one
                        val formattedText = if (text.endsWith('.') || text.endsWith('。') || text.endsWith('!') || text.endsWith('?') || text.endsWith('！') || text.endsWith('？')) {
                            text
                        } else {
                            "$text。"
                        }
                        // 计算进度百分比和耗时
                        val progress = 100 // 完成所有处理
                        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
                        runOnUiThread {
                            lastText = "${lastText}\n音频文件识别结果: [进度: $progress%, 耗时: %.1f秒] $formattedText".format(elapsedTime)
                            idx += 1
                            textView.append("\n音频文件识别结果: [进度: $progress%, 耗时: %.1f秒] $formattedText".format(elapsedTime))
                        }
                    }
                    vad.pop()
                }

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
                    // 为语音段添加末尾padding，避免句子末尾丢字问题
                    val paddedSamples = addPaddingToSegment(segment.samples)
                    coroutineScope.launch {
                        val text = runSecondPass(paddedSamples)
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

    private fun initOfflineRecognizer() {
        // Please change getOfflineModelConfig() to add new models
        // See https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html
        // for a list of available models
        //sherpa-onnx-fire-red-asr-large-zh_en-2025-02-16
        val asrModelType = 24
        val asrRuleFsts: String?
        asrRuleFsts = null
        Log.i(TAG, "Select model type ${asrModelType} for ASR")

        val config = OfflineRecognizerConfig(
            featConfig = getFeatureConfig(sampleRate = sampleRateInHz, featureDim = 80),
            modelConfig = getOfflineModelConfig(type = asrModelType)!!,
        )
        if (asrRuleFsts != null) {
            config.ruleFsts = asrRuleFsts;
        }

        offlineRecognizer = OfflineRecognizer(
            assetManager = application.assets,
            config = config,
        )
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
}