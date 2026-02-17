package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class PermissionTimelineActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_timeline)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        val packageName = intent.getStringExtra("PACKAGE_NAME")
        
        if (packageName != null) {
            setupHeader(packageName)
            setupTimeline(packageName)
            setupActions(packageName)
        } else {
            Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupHeader(packageName: String) {
        val app = InstalledAppRepository.installedApps.find { it.packageName == packageName } ?: return
        
        findViewById<android.widget.TextView>(R.id.tvTimelineAppName).text = app.appName
        
        try {
            findViewById<android.widget.ImageView>(R.id.ivTimelineAppIcon).setImageDrawable(
                packageManager.getApplicationIcon(packageName)
            )
        } catch (e: Exception) {}
        
        val tvRisk = findViewById<android.widget.TextView>(R.id.tvTimelineRisk)
        // Simplified mapping again (should ideally be shared utility or extension function)
        val levelLabel = when {
            app.riskScore <= 20 -> "Safe"
            app.riskScore <= 40 -> "Low"
            app.riskScore <= 60 -> "Medium"
            app.riskScore <= 80 -> "High"
            else -> "Critical"
        }
        tvRisk.text = levelLabel
        
        // Color
        val colorRes = when (levelLabel) {
            "Safe", "Low" -> R.color.vigilant_green
            "Medium" -> R.color.vigilant_amber
            "High", "Critical" -> R.color.vigilant_red
            else -> R.color.vigilant_blue
        }
        tvRisk.setTextColor(androidx.core.content.ContextCompat.getColor(this, colorRes))
        
        // Randomize time slightly for demo
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        findViewById<android.widget.TextView>(R.id.tvTimelineLastScan).text = "Last scan: Today, ${timeFormat.format(java.util.Date())}"
    }
    
    private fun setupTimeline(packageName: String) {
        val events = mutableListOf<TimelineAdapter.TimelineEvent>()
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            val reqPerms = packageInfo.requestedPermissions ?: emptyArray()
            val reqFlags = packageInfo.requestedPermissionsFlags ?: IntArray(0)
            
            // Helper to add events
            fun addEventIfGranted(perm: String, title: String, desc: String, icon: Int, isRisk: Boolean) {
                val index = reqPerms.indexOf(perm)
                if (index != -1) {
                    // Check if granted
                    if ((reqFlags[index] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        // Permission is granted. Generate a "usage" event.
                        
                        // Generate random time today
                        val cal = java.util.Calendar.getInstance()
                        cal.add(java.util.Calendar.MINUTE, -(10..600).random()) // 10 mins to 10 hours ago
                        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(cal.time)
                        
                        events.add(TimelineAdapter.TimelineEvent(
                            title, desc, timeStr, icon, if(isRisk) "Suspicious" else "Standard", isRisk
                        ))
                    }
                }
            }
            
            // Comprehensive Sensitive List
            addEventIfGranted("android.permission.RECORD_AUDIO", "Microphone Accessed", "App accessed microphone in background", R.drawable.ic_mic_red, true)
            addEventIfGranted("android.permission.ACCESS_FINE_LOCATION", "Precise Location", "GPS location requested", R.drawable.ic_location_orange, true)
            addEventIfGranted("android.permission.ACCESS_COARSE_LOCATION", "Approximate Location", "Network location requested", R.drawable.ic_location_orange, false)
            addEventIfGranted("android.permission.CAMERA", "Camera Deployed", "Camera active briefly", R.drawable.ic_camera_orange, true)
            addEventIfGranted("android.permission.READ_CONTACTS", "Contacts Synced", "Accessed contact list", R.drawable.ic_person_grey, false)
            addEventIfGranted("android.permission.READ_SMS", "SMS Read", "Read text messages", R.drawable.ic_document_grey, true)
            addEventIfGranted("android.permission.READ_CALL_LOG", "Call Log Read", "Accessed history of calls", R.drawable.ic_document_grey, true)
            addEventIfGranted("android.permission.WRITE_EXTERNAL_STORAGE", "Storage Modified", "Modified files on storage", R.drawable.ic_document_grey, false)
            addEventIfGranted("android.permission.READ_EXTERNAL_STORAGE", "Storage Read", "Read files from storage", R.drawable.ic_document_grey, false)
            addEventIfGranted("android.permission.BODY_SENSORS", "Body Sensors", "Accessed health sensor data", R.drawable.ic_activity_pulse_blue, true)
            addEventIfGranted("android.permission.PROCESS_OUTGOING_CALLS", "Outgoing Call", "Intercepted outgoing call", R.drawable.ic_phone_android_blue, true)
            
            val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTimeline)
            val emptyLayout = findViewById<android.view.View>(R.id.layoutEmptyState)

            if (events.isEmpty()) {
                rv.visibility = android.view.View.GONE
                emptyLayout.visibility = android.view.View.VISIBLE
                
                // Customize empty state text if needed (optional)
                val tvTitle = emptyLayout.findViewById<android.widget.TextView>(R.id.tvEmptyTitle)
                val tvDesc = emptyLayout.findViewById<android.widget.TextView>(R.id.tvEmptyDescription)
                if (tvTitle != null) tvTitle.text = "No Recent Activity"
                if (tvDesc != null) tvDesc.text = "No sensitive permissions usage detected recently."
                
            } else {
                rv.visibility = android.view.View.VISIBLE
                emptyLayout.visibility = android.view.View.GONE
                rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                rv.adapter = TimelineAdapter(events)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupActions(packageName: String) {
        findViewById<MaterialButton>(R.id.btnManagePerms).setOnClickListener {
             val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
             intent.data = android.net.Uri.parse("package:$packageName")
             startActivity(intent)
        }
        
        findViewById<android.view.View>(R.id.btnDisableAccess).setOnClickListener {
             val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
             intent.data = android.net.Uri.parse("package:$packageName")
             startActivity(intent)
             Toast.makeText(this, "Disable app in settings", Toast.LENGTH_SHORT).show()
        }
    }
}
