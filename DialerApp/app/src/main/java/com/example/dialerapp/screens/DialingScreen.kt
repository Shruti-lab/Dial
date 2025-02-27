package com.example.dialerapp.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.ContentValues.TAG
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import com.example.dialerapp.R
import com.example.dialerapp.utils.CallManager
import com.example.dialerapp.utils.PhoneNumberFormatter

class DialingScreen : AppCompatActivity() {
    private lateinit var phoneNumberDisplay: TextView
    private lateinit var btnCall: Button
    private val dialPadButtons = mutableListOf<Button>()
    private val phoneNumberBuilder = StringBuilder()
    private val TAG = "DIALING SCREEN"

    companion object {
        const val REQUEST_CODE_SET_DEFAULT_DIALER=200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialingscreen)

        initializeViews()
        Log.e(TAG, "About to check if we're the default dialer")  // Using ERROR level for visibility
        setupDialPadListeners()
        setupCallButton()

    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume - checking default dialer status")
    }

    private fun initializeViews() {
        phoneNumberDisplay = findViewById(R.id.phoneNumberDisplay)
        btnCall = findViewById(R.id.btnCall)

        // Collect all dial pad buttons
        val buttonIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnStar, R.id.btnHash
        )

        buttonIds.forEach { id ->
            val button = findViewById<Button>(id)
            dialPadButtons.add(button)
        }
    }

    private fun setupDialPadListeners() {
        dialPadButtons.forEach { button ->
            button.setOnClickListener {
                addDigitToPhoneNumber(button.text.toString())
            }
        }
    }

    private fun addDigitToPhoneNumber(digit: String) {
        // Limit phone number length and format
        if (phoneNumberBuilder.length < 11) {
            phoneNumberBuilder.append(digit)
            updatePhoneNumberDisplay()
        }
    }

    private fun updatePhoneNumberDisplay() {
        phoneNumberDisplay.text = PhoneNumberFormatter.format(phoneNumberBuilder.toString())
    }

    private fun setupCallButton() {
        btnCall.setOnClickListener {
            val phoneNumber = phoneNumberBuilder.toString()

            if (phoneNumber.isNotBlank()) {
  //              Check and request call permissions
                makeCall(phoneNumber)
//                if (checkCallPermissions()) {
//                    makeCall(phoneNumber)
//                }
            }
        }
    }

//    private fun checkCallPermissions(): Boolean {
//        // Reuse permission checking logic from MainActivity
//        val requiredPermissions = arrayOf(
//            Manifest.permission.READ_PHONE_STATE
//        )
//
//        val missingPermissions = requiredPermissions.filter { permission ->
//            ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED
//        }
//
//        return if (missingPermissions.isEmpty()) {
//            true
//        } else {
//            ActivityCompat.requestPermissions(
//                this,
//                missingPermissions.toTypedArray(),
//                CALL_PERMISSION_REQUEST_CODE
//            )
//            false
//        }
//    }


    private fun makeCall(phoneNumber: String) {
        try {
            // Use CallManager to handle call initiation and recording
            Toast.makeText(this,
                "now initialing call",
                Toast.LENGTH_LONG
            ).show()
            CallManager.initiateCall(this, phoneNumber)
        } catch (e: SecurityException) {
            // This shouldn't happen since permissions are already granted
            val msg:String = e.message?:"Nothing-------------";
            Log.e(TAG,msg)
            println(e.message)
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()

            finish()
        }
    }

//    companion object {
//        private const val CALL_PERMISSION_REQUEST_CODE = 200
//    }

}
