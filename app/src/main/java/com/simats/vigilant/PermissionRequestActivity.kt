package com.simats.vigilant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionRequestActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100
    
    // List of permissions needed for the app
    private val requiredPermissions = mutableListOf<String>().apply {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    // Modern Activity Result API for Usage Stats permission
    private val usageStatsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check if permission was granted
        if (hasUsageStatsPermission()) {
            Toast.makeText(this, "Usage Access granted", Toast.LENGTH_SHORT).show()
        }
        checkSpecialPermissionsSequence()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_request)
        
        // Setup UI
        setupUI()
    }
    
    override fun onResume() {
        super.onResume()
        // If we are coming back from settings, we might need to update state or auto-proceed
        // But for this flow, we control it better via user interaction to avoid loops
    }
    
    private fun setupUI() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGrantPermissions).setOnClickListener {
            startPermissionFlow()
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSkip).setOnClickListener {
            showSkipWarningDialog()
        }
    }
    
    private fun startPermissionFlow() {
        // Step 1: Runtime Permissions (Cam, Mic, Notifications)
        if (!hasRuntimePermissions()) {
            requestRuntimePermissions()
        } else {
            // Step 2: Special Permissions Sequence
            checkSpecialPermissionsSequence()
        }
    }

    // --- Runtime Permissions ---

    private fun hasRuntimePermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
             // Regardless of result, proceed to special permissions
             // We can show a toast if some were denied
             if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                 Toast.makeText(this, "Some basic permissions denied. Proceeding to security permissions.", Toast.LENGTH_SHORT).show()
             } else {
                 Toast.makeText(this, "Basic permissions granted.", Toast.LENGTH_SHORT).show()
             }
             checkSpecialPermissionsSequence()
        }
    }

    // --- Special Permissions Sequence ---
    
    private fun checkSpecialPermissionsSequence() {
        // Priority 1: Usage Access
        if (!hasUsageStatsPermission()) {
            showUsageAccessDialog()
            return
        }
        
        // Priority 2: Accessibility Service
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityDialog()
            return
        }
        
        // Priority 3: Notification Listener
        if (!isNotificationServiceEnabled()) {
             showNotificationAccessDialog()
             return
        }
        
        // All Done
        finishPermissionFlow()
    }
    
    // 1. Usage Access
    private fun showUsageAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Usage Access Required")
            .setMessage("Vigilant needs Usage Access to detect spyware running in the background and identify suspicious app behavior.")
            .setPositiveButton("Grant") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
                // Mark skipped or just proceed to next check
                // For this strict flow, we just proceed to next check, effectively skipping
                // In a real app we might store "usage_skipped" boolean
                // Temporarily disable check for recursion? No, the check happens on button click/resume. 
                // We need a way to move to the next step immediately without looping.
                // We will use a recursive check but assume user action triggers re-check or next step.
                // To flow smoothly, simply re-call sequence? No, that would loop.
                // We'll trust the user to come back or click "Grant" again. 
                // But requested flow is sequential. Let's just prompt for the next one if they skip?
                // For simplicity: If they skip, we assume they want to move on.
                // But we don't have state for "skipped this specific one".
                // Let's rely on the Button "Grant Permissions" to trigger the flow again.
                // The prompt says "Request permissions only after explanation". 
                
                // Let's try to proceed to next blindly?
                promptNextAfterUsage()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun promptNextAfterUsage() {
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityDialog()
        } else if (!isNotificationServiceEnabled()) {
            showNotificationAccessDialog()
        } else {
            finishPermissionFlow()
        }
    }

    // 2. Accessibility
    private fun showAccessibilityDialog() {
         AlertDialog.Builder(this)
            .setTitle("Accessibility Service Required")
            .setMessage("Vigilant requires Accessibility Service to detect when other apps try to interact with your screen securely or perform unauthorized clicks. \n\nWe DO NOT read your personal content or track keystrokes.")
            .setPositiveButton("Grant") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
                promptNextAfterAccessibility()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun promptNextAfterAccessibility() {
         if (!isNotificationServiceEnabled()) {
            showNotificationAccessDialog()
        } else {
            finishPermissionFlow()
        }
    }

    // 3. Notification Access
    private fun showNotificationAccessDialog() {
         AlertDialog.Builder(this)
            .setTitle("Notification Access Required")
            .setMessage("Vigilant needs to monitor notifications to detect spyware that tries to hide its presence or intercept your alerts.")
            .setPositiveButton("Grant") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } else {
                     startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
                finishPermissionFlow()
            }
            .setCancelable(false)
            .show()
    }

    // --- Checks ---

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
    
    private fun isNotificationServiceEnabled(): Boolean {
        val componentName = android.content.ComponentName(this, com.simats.vigilant.services.VigilantNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(componentName.flattenToString())
    }

    // --- Navigation ---

    private fun finishPermissionFlow() {
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_launch", false).apply()
        navigateToWelcome()
    }

    private fun showSkipWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle("Skip Permissions?")
            .setMessage("Without proper permissions, Vigilant cannot protect your device from threats. Are you sure you want to skip?")
            .setPositiveButton("Yes, Skip") { _, _ ->
                 finishPermissionFlow()
            }
            .setNegativeButton("Grant Permissions") { dialog, _ ->
                dialog.dismiss()
                startPermissionFlow()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun navigateToWelcome() {
        val intent = Intent(this, ScanIntroActivity::class.java)
        startActivity(intent)
        finish()
    }
}
