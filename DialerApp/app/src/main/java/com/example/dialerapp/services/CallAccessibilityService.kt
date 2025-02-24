package com.example.dialerapp.services

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ForegroundServiceStartNotAllowedException
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.PermissionChecker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallAccessibilityService : AccessibilityService() {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private var lastCallScreenTime = 0L
    private var isCallActive = false
    private val DEBOUNCE_TIME = 1000L // 1 second debounce
    private var lastEventTime = 0L


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

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
            Log.e(TAG, "Accessibility permission NOT GRANTED SO STOPPING")


            stopSelf()
            return
        }

        try {
            val notification = NotificationCompat.Builder(this, "AccessibilityServiceChannelId")
                // Create the notification to display while the service is running
                .build()
            ServiceCompat.startForeground(
                this,
                101,
               notification,
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (Build.VERSION.SDK_INT < 29) {
            stopSelf() // Stop this service on SDK < 29
            return
        }

        startForeground()




        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = arrayOf("com.android.phone", "com.android.server.telecom","com.samsung.android.incallui", "com.google.android.dialer")
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d(TAG, "Accessibility Service Connected")
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "Event Type: ${event.eventType}, Package: ${event.packageName}, Class: ${event.className}")

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {


            val currentTime = System.currentTimeMillis()
            // Prevent processing multiple events too quickly
            if (currentTime - lastEventTime < 100) {
                return
            }
            lastEventTime = currentTime

            val currentPackage = event.packageName?.toString()
            val currentClass = event.className?.toString()

            Log.d(TAG, "Window state changed - Package: $currentPackage, Class: $currentClass")

            if (!isCallPackage(currentPackage)) {
                Log.d(TAG, "Not a call package, checking for recording stop")
                handleNonCallScreen()
                return
            }

            if (!isCallScreenClass(currentClass)) {
                Log.d(TAG, "Not a call screen class, checking for recording stop")
                handleNonCallScreen()
                return
            }

            // Valid call screen detected
            lastCallScreenTime = currentTime
            if (!isCallActive) {
                isCallActive = true
                Log.d(TAG, "Call became active - Last screen time: $lastCallScreenTime")
                if (!isRecording) {
                    Log.d(TAG, "Starting recording - Call screen detected")
                    startRecording()
                }
            }
        }
    }

    private fun isCallPackage(packageName: String?): Boolean {
        val callPackages = setOf(
            "com.android.phone",
            "com.android.server.telecom",
            "com.samsung.android.incallui",
            "com.google.android.dialer"
        )
        return packageName in callPackages
    }

    private fun isCallScreenClass(className: String?): Boolean {
        if (className == null) return false

        val name = className.lowercase()
        return name.contains("incall") ||
                name.contains("incomingcall") ||
                name.contains("oncall") ||
                name.contains("callactivity") ||
                name.contains("callscreen")
    }

    private fun handleNonCallScreen() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCallScreen = currentTime - lastCallScreenTime

        Log.d(TAG, "Checking non-call screen - Time since last call screen: $timeSinceLastCallScreen ms")

        // Only stop recording if we've been away from call screen for more than the debounce time
        if (isCallActive && timeSinceLastCallScreen > DEBOUNCE_TIME) {
            Log.d(TAG, "Call screen no longer active - stopping recording")
            isCallActive = false
            if (isRecording) {
                stopRecording()
            }
        }
    }



    private fun startRecording() {
        if (isRecording) {
            Log.d(TAG, "Recording already in progress - skipping start")
            return
        }


        try {
            // Create app-specific media directory if it doesn't exist
            val audioDir = File(getExternalFilesDir(null), "CallRecordings").apply {
                if (!exists()) {
                    mkdirs()
                }
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Call_Recording_$timestamp.mp3"
            outputFile = File(audioDir, fileName)

            mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started (Accessibility): ${outputFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            cleanupRecording()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        try {
            Log.d(TAG, "Attempting to stop recording")

            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            Log.d(TAG, "Recording stopped(Accessibility): ${outputFile?.absolutePath}")
//            Toast.makeText(this, "Call recording saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            Toast.makeText(this, "Error saving recording", Toast.LENGTH_SHORT).show()
        } finally {
            cleanupRecording()
        }
    }

    private fun cleanupRecording() {
        Log.d(TAG, "Cleaning up recording resources")
        mediaRecorder = null
        isRecording = false
        outputFile = null
        isCallActive = false

    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
        stopRecording()
    }

    override fun onDestroy() {
        stopRecording()
        Log.d(TAG, "Accessibility Service being destroyed - Final state: recording=$isRecording, callActive=$isCallActive")
        Log.d(TAG, "Accessibility Service being destroyed")
        if (isRecording) {
            stopRecording()
        }
        isCallActive = false
        super.onDestroy()
    }
}

