package com.example.dialerapp.screens

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.dialerapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123

        // Basic permissions needed for all Android versions
        private val BASE_PERMISSIONS = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MANAGE_OWN_CALLS,
        )

        // Permissions needed for Android 8.0 (API 26) and above
        @RequiresApi(Build.VERSION_CODES.O)
        private val ANDROID_8_PERMISSIONS = arrayOf(
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_PHONE_NUMBERS
        )

        // Storage permissions (NOT NEEDED)
        private val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }
    // Dialog references
    private var permissionRationaleDialog: AlertDialog? = null
    private var limitedFunctionalityDialog: AlertDialog? = null
    private var accessibilityServiceDialog: AlertDialog? = null
    private var limitedFunctionalityDialogForAccessibility: AlertDialog? = null

    private val requiredPermissions: Array<String>
        get() {
            val permissions = mutableListOf<String>()

            // Add base permissions
            permissions.addAll(BASE_PERMISSIONS)

            // Add version-specific permissions
            when {
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//                    // Add Android 12 (API 31) and above permissions if any (like bluetooth one)
//                    permissions.addAll(ANDROID_8_PERMISSIONS)
//                }
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
//                    // Android 8 (API 26) and above permissions till API 31
//                    permissions.addAll(ANDROID_8_PERMISSIONS)
//                }
//                else -> run{
//                    // Below Android 8
//                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Add POST_NOTIFICATIONS permission for Android 13+
                    permissions.addAll(ANDROID_8_PERMISSIONS)
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // Android 8 (API 26) and above permissions till API 31
                    permissions.addAll(ANDROID_8_PERMISSIONS)
                }
                else -> run{
                    // Below Android 8
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            return permissions.toTypedArray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check permissions when activity creates
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
//        Android 6.0/API 23 and above need runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Get list of permissions that aren't granted
            val permissionsToRequest = requiredPermissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }

            when {
                permissionsToRequest.isEmpty() -> {
                    // All permissions are granted
                    initializeApp()
                }
                shouldShowRequestPermissionRationale(permissionsToRequest.first()) -> {
                    // Show explanation why permissions are needed
                    showPermissionRationaleDialog(permissionsToRequest.toTypedArray())
                }
                else -> {
                    // Request permissions directly
                    requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
                }
            }
        } else {
            // Below Android 6, permissions are granted at install time
            initializeApp()
        }
    }

    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        permissionRationaleDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs access to your phone and storage to record calls. " +
                    "Without these permissions, call recording won't work properly.")
            .setPositiveButton("Grant") { _, _ ->
                requestPermissions(permissions, PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Show limited functionality or close app
                showLimitedFunctionalityDialog()
            }
            .show()
    }

    private fun showLimitedFunctionalityDialog() {
        limitedFunctionalityDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Limited Functionality")
            .setMessage("The app will run with limited functionality without the required permissions. You can grant permissions later from the app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Continue Anyway") { _, _ ->
                initializeAppWithLimitedFunctionality()
            }
            .show()
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            when {
                deniedPermissions.isEmpty() -> {
                    // All permissions granted
                    initializeApp()
                }
                deniedPermissions.any {
                    shouldShowRequestPermissionRationale(it)
                } -> {
                    // Some permissions denied but can ask again
                    showPermissionRationaleDialog(deniedPermissions.toTypedArray())
                }
                else -> {
                    // Permissions denied with "Don't ask again"
                    Toast.makeText(this,
                        deniedPermissions.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                    showLimitedFunctionalityDialog()
                }
            }
        }
    }

    private fun initializeApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // For Android 9+, check if accessibility service is enabled
            checkAccessibilityService()

        } else {
            // For lower versions, proceed with normal initialization
            navigateToDialingScreen()
        }
    }

    private fun initializeAppWithLimitedFunctionality() {
        // Initialize with limited features based on granted permissions
        Toast.makeText(this,
            "Some features may not work without required permissions",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun checkAccessibilityService() {
        Log.d(TAG, "before checking accessibility service enable")
        val accessibilityManager = getSystemService(AccessibilityManager::class.java)
        val isAccessibilityEnabled = accessibilityManager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
        Log.d(TAG, "mid section accessibility section------")

        if (!isAccessibilityEnabled) {
            Log.d(TAG, "accessibility service if not enabled(inside if)")
            // If the accessibility service is not enabled, increment denial count
            incrementDenialCount()

            // Check if the user has denied twice
            if (getDenialCount() >= 2) {
                showLimitedFunctionalityDialogForAccessibility()
            } else {
                showAccessibilityServiceDialog()
            }


        } else {
            Log.d(TAG, "Daamn All done!")
            Toast.makeText(this, "Accessibility service is enabled", Toast.LENGTH_LONG).show()
            navigateToDialingScreen()
            resetDenialCount()  // Reset denial count when service is enabled
            Log.d(TAG, "after accessibility service enabled...YAY!")
        }


    }

    private fun showAccessibilityServiceDialog() {
        accessibilityServiceDialog = MaterialAlertDialogBuilder(this)
            .setTitle("Accessibility Service Required")
            .setMessage("Call recording requires accessibility service to be enabled. " +
                    "Please enable it in the settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel"){_, _ ->
                showLimitedFunctionalityDialogForAccessibility()
            }
            .show()
    }

    private fun showLimitedFunctionalityDialogForAccessibility() {
        limitedFunctionalityDialogForAccessibility = MaterialAlertDialogBuilder(this)
            .setTitle("Accessibility Services Needed")
            .setMessage("The app will not run without the accessibility permissions.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Continue Anyway") { _, _ ->
                initializeAppWithLimitedFunctionality()
            }
            .show()
    }

    private fun navigateToDialingScreen() {
        // Start DialingScreen and finish MainActivity
        startActivity(Intent(this, DialingScreen::class.java))
        finish() // This ensures user can't go back to permission screen
    }

    private fun getDenialCount(): Int {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        return sharedPreferences.getInt("accessibility_denials", 0)
    }

    private fun incrementDenialCount() {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val denialCount = getDenialCount() + 1
        sharedPreferences.edit().putInt("accessibility_denials", denialCount).apply()
    }

    private fun resetDenialCount() {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        sharedPreferences.edit().putInt("accessibility_denials", 0).apply()
    }
    override fun onResume() {
        super.onResume()

        // Check the accessibility service again when the activity resumes
        checkAccessibilityService()
    }

//    override fun finish() {
//        val view = window.decorView as ViewGroup
//        view.removeAllViews()
//        super.finish()
//    }
    override fun onDestroy() {
        super.onDestroy()
        if(permissionRationaleDialog!= null &&  permissionRationaleDialog!!.isShowing){
            permissionRationaleDialog!!.dismiss()
        }
        if (limitedFunctionalityDialog != null && limitedFunctionalityDialog!!.isShowing) {
            limitedFunctionalityDialog!!.dismiss()
        }
        if (accessibilityServiceDialog != null && accessibilityServiceDialog!!.isShowing) {
            accessibilityServiceDialog!!.dismiss()
        }
        if (limitedFunctionalityDialogForAccessibility != null && limitedFunctionalityDialogForAccessibility!!.isShowing) {
            limitedFunctionalityDialogForAccessibility!!.dismiss()
        }

    }

}

