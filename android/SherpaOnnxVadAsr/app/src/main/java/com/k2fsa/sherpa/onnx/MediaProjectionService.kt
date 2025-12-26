package com.k2fsa.sherpa.onnx

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MediaProjectionService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "MediaProjectionChannel"
        const val ACTION_START = "com.k2fsa.sherpa.onnx.START_MEDIA_PROJECTION"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        const val BROADCAST_MEDIA_PROJECTION_READY = "com.k2fsa.sherpa.onnx.MEDIA_PROJECTION_READY"
        const val EXTRA_MEDIA_PROJECTION = "mediaProjection"
    }

    private val TAG = "MediaProjectionService"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                }

                // Start foreground service with notification
                val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("System Audio Recording")
                    .setContentText("Capturing system audio in progress")
                    .setSmallIcon(android.R.drawable.ic_menu_manage)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                startForeground(NOTIFICATION_ID, notification)

                // Send the result code and data back to the activity instead of the media projection
                if (resultCode != 0 && resultData != null) {
                    try {
                        val broadcastIntent = Intent(BROADCAST_MEDIA_PROJECTION_READY)
                        broadcastIntent.putExtra(EXTRA_RESULT_CODE, resultCode)
                        broadcastIntent.putExtra(EXTRA_RESULT_DATA, resultData)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
                        
                        Log.i(TAG, "Media projection result code and data sent successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending media projection data: ${e.message}")
                    }
                }

                // We keep the service running in foreground for the duration of the recording
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Media Projection Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}