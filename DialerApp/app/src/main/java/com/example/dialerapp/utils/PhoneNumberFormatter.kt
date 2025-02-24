package com.example.dialerapp.utils

object PhoneNumberFormatter {
    fun format(phoneNumber: String): String {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")

        return cleanNumber
    }
}