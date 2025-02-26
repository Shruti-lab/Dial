//package com.example.dialerapp.receivers
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.telephony.TelephonyManager
//import com.example.dialerapp.utils.CallManager
//
//class CallStateReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
//            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
//
//            when (state) {
//                TelephonyManager.EXTRA_STATE_IDLE -> {
//                    CallManager.handleCallEnded(context)
//                }
//                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
//                    // Call is active
//                }
//                TelephonyManager.EXTRA_STATE_RINGING -> {
//                    // Phone is ringing
//                }
//            }
//        }
//    }
//}