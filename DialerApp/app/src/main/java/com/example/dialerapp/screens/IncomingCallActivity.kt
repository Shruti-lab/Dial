package com.example.dialerapp.screens

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.dialerapp.R
import com.example.dialerapp.utils.CallManager
import com.example.dialerapp.utils.PhoneNumberFormatter

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var callerName: TextView
    private lateinit var phoneNumberTextView: TextView
    private lateinit var answerButton: Button
    private lateinit var rejectButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

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
        callerName = findViewById(R.id.callerNameTextView)
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView)
        answerButton = findViewById(R.id.answerButton)
        rejectButton = findViewById(R.id.rejectButton)
    }

    private fun setupCallInfo() {
        val phoneNumber = intent.getStringExtra("CALL_NUMBER") ?: "Unknown"
        phoneNumberTextView.text = PhoneNumberFormatter.format(phoneNumber)
        // In a real app, you would look up the contact name here
        callerName.text = "Incoming Call"
    }

    private fun setupButtonListeners() {
        answerButton.setOnClickListener {
            CallManager.answerCall(this)
            finish()
        }

        rejectButton.setOnClickListener {
            CallManager.rejectCall(this)
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