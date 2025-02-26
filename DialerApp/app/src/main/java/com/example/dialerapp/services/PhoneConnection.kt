package com.example.dialerapp.services

import android.content.Context
import android.content.Intent
import android.media.ToneGenerator
import android.os.Build
import android.telecom.Call.STATE_DISCONNECTING
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.DisconnectCause.REJECTED
import android.util.Log
import com.example.dialerapp.screens.ActiveCallActivity
import com.example.dialerapp.screens.DialingScreen
import com.example.dialerapp.screens.IncomingCallActivity
import com.example.dialerapp.screens.OutgoingCallActivity


class PhoneConnection(context: Context): Connection() {
    private val TAG = "PhoneConnection"
    private var isRecordingStarted = false
    private var dtmfGenerator :ToneGenerator? = null

    val applicationContext = context.applicationContext


    override fun onAnswer() {
        Log.d(TAG,"Call answered")
        setActive()
        startCallRecording()
    }

    override fun onDisconnect() {
        Log.d(TAG,"Call disconnected")
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL, "Call disconnected"))
        stopCallRecordingIfActive()
        destroy()

    }

    override fun onReject() {
        setDisconnected(DisconnectCause(REJECTED, "Rejecting call"))
        stopCallRecordingIfActive()
        destroy()
    }

    override fun onHold() {
        Log.d(TAG, "Call on hold")
        setOnHold()
        super.onHold()
    }

    override fun onUnhold() {
        Log.d(TAG, "Call removed from hold")
        setActive()
        super.onUnhold()
    }

    override fun onShowIncomingCallUi() {


        Log.d(TAG, "Showing incoming call UI for number: ${address?.schemeSpecificPart}")
        val incomingCallIntent = Intent(applicationContext, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("CALL_ADDRESS", address?.schemeSpecificPart)
            putExtra("CALL_NUMBER", address?.schemeSpecificPart)
        }
        applicationContext.startActivity(incomingCallIntent)
    }

    private fun startCallRecording() {
        if (isRecordingStarted) {
            Log.w(TAG, "Call recording already started, skipping")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and above - Use CallAccessibilityService
            Log.d(TAG, "Using CallAccessibilityService for Android 10+")
            Intent(applicationContext, CallAccessibilityService::class.java)
        } else {
            // Android 9 and below - Use direct recording
            Log.d(TAG, "Using CallRecordingService for Android 9 and below")

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(Intent(applicationContext, CallRecordingService::class.java))
                } else {
                    applicationContext.startService(Intent(applicationContext, CallRecordingService::class.java))
                }
                isRecordingStarted = true
                Log.d(TAG, "Call recording started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start call recording: ${e.message}")
            }
        }


    }

    private fun stopCallRecordingIfActive() {
        if (!isRecordingStarted) {
            Log.d(TAG, "No active recording to stop")
            return
        }

        val serviceIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(applicationContext, CallAccessibilityService::class.java)
        } else {
            Intent(applicationContext, CallRecordingService::class.java)
        }

        try {
            applicationContext.stopService(serviceIntent)
            isRecordingStarted = false
            Log.d(TAG, "Call recording stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop call recording: ${e.message}")
        }
    }

    private fun startCallTone(){
//        try {
//            dtmfGenerator = ToneGenerator(ToneGenerator.TONE_SUP_RINGTONE, ToneGenerator.MAX_VOLUME)
//            dtmfGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 1000)
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to initialize ToneGenerator: ${e.message}")
//            // Optionally, show a message to the user or handle the failure
//        }

        try {
            // Only initialize ToneGenerator if it hasn't been initialized yet
            if (dtmfGenerator == null) {
                dtmfGenerator = ToneGenerator(ToneGenerator.TONE_SUP_RINGTONE, ToneGenerator.MAX_VOLUME)
            }

            // Start the tone only if it's initialized properly
            if (dtmfGenerator != null) {
                dtmfGenerator?.startTone(ToneGenerator.TONE_DTMF_0, 1000)
            } else {
                Log.e(TAG, "Failed to initialize ToneGenerator.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call tone: ${e.message}")
        }

    }


    private fun stopCallTone(){
        dtmfGenerator?.stopTone()

    }

    override fun onStateChanged(state: Int) {
        when(state){
            STATE_RINGING -> Log.d(TAG, "Call ringing")

            STATE_DIALING -> {
                Log.d(TAG, "Call dialing")
                Log.d(TAG, "Launching OutgoingCallActivity")
                val outgoingCallIntent = Intent(applicationContext, OutgoingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra("CALL_NUMBER", "9405614793")
                }
                applicationContext.startActivity(outgoingCallIntent)
//                startCallTone()
            }

            STATE_ACTIVE -> {
                Log.d(TAG, "Call active")
                startCallRecording()
                val intent = Intent(applicationContext, ActiveCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra("CALL_NUMBER", address?.schemeSpecificPart)
                    putExtra("CALL_STATE", "ACTIVE")
                }
                applicationContext.startActivity(intent)
//                stopCallTone()
            }

            STATE_DISCONNECTING -> {
                stopCallRecordingIfActive()
                Log.d(TAG, "Call disconnecting")
            }

            STATE_DISCONNECTED ->{
                stopCallRecordingIfActive()
                Log.d(TAG, "Call state: DISCONNECTED - Returning to DialingScreen")
                applicationContext.startActivity(Intent(applicationContext, DialingScreen::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
            }

        }

    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(state: CallAudioState) {
        Log.d(TAG, "Call audio state changed - Route: ${state.route}, Muted: ${state.isMuted}")
    }




}