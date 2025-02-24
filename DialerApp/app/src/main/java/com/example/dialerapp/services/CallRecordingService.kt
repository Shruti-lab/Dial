package com.example.dialerapp.services

import android.app.Service.START_NOT_STICKY
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import androidx.core.app.ServiceCompat.startForeground
import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioRecord
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.PermissionChecker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {
    private val CHANNEL_ID = "CallRecordingChannel"
    private val NOTIFICATION_ID = 2
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null

    override fun onCreate() {
        super.onCreate()
        startRecording()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        when (intent?.action) {
//            "START_RECORDING" -> startRecording()
//            "STOP_RECORDING" -> stopRecording()
//        }
//        val notification = createNotification()
//        startForeground(NOTIFICATION_ID, notification)
        startForeground()

        return START_STICKY
    }

    private fun startForeground() {
        // Before starting the service as foreground check that the app has the
        // appropriate runtime permissions. In this case, verify that the user has
        // granted the AUDIO permission.

        if (PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {

            stopSelf()
            return
        }

        try {
            val notification = NotificationCompat.Builder(this, "CHANNEL_ID")
                // Create the notification to display while the service is running
                .build()
            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ 101,
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                return
            }
            // ...
        }
    }

    private fun startRecording() {
        if (isRecording) return

        val fileDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "CallRecordings").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        outputFile = File(fileDir, "Call_Recording_$timestamp.mp3")

        try {
            if (Build.VERSION.SDK_INT in 24..28) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    Log.d(TAG,"Permission not given")

                    return
                }
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    44100,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(44100, android.media.AudioFormat.CHANNEL_IN_MONO, android.media.AudioFormat.ENCODING_PCM_16BIT)
                )
                audioRecord?.startRecording()

                isRecording = true
                Log.d("CallRecordingService", "Recording started: ${outputFile?.absolutePath}")
            }

        } catch (e: Exception) {
            Log.e("CallRecordingService", "Error starting recording", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    private fun stopRecording() {
        if (!isRecording) return

        try {
//            mediaRecorder?.stop()
//            mediaRecorder?.release()
            audioRecord?.stop()
            audioRecord?.release()
            Log.d("CallRecordingService", "Recording saved: ${outputFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("CallRecordingService", "Error stopping recording", e)
        } finally {
            mediaRecorder = null
            audioRecord = null
            isRecording = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null


}
