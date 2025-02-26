package com.example.dialerapp.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dialerapp.R
import com.example.dialerapp.utils.CallManager
import com.example.dialerapp.utils.PhoneNumberFormatter

class OutgoingCallActivity : AppCompatActivity() {
    private lateinit var callerNumberTextView: TextView
    private lateinit var callStatusTextView: TextView
    private lateinit var callDurationTextView: TextView
    private lateinit var endCallButton: Button
    private var callDuration = 0
    private val handler = Handler(Looper.getMainLooper())
    private var durationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outgoing_call)

        // Keep screen on and show above lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        initializeViews()
        setupCallInfo()
        setupButtonListeners()
    }

    private fun initializeViews() {
        callerNumberTextView = findViewById(R.id.callerNumberTextView)
        callStatusTextView = findViewById(R.id.callStatusTextView)
        callDurationTextView = findViewById(R.id.callDurationTextView)
        endCallButton = findViewById(R.id.endCallButton)
    }

    private fun setupCallInfo() {
        val phoneNumber = intent.getStringExtra("CALL_NUMBER") ?: "Unknown"
        callerNumberTextView.text = PhoneNumberFormatter.format(phoneNumber)
        callStatusTextView.text = "Calling..."
    }

    private fun setupButtonListeners() {
        endCallButton.setOnClickListener {
            CallManager.endCall(this)
            finish()
        }
    }





    override fun onDestroy() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        super.onDestroy()
    }
}
