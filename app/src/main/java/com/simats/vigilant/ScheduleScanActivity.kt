package com.simats.vigilant

import android.os.Bundle
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class ScheduleScanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_scan)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }

        val switchEnable = findViewById<SwitchMaterial>(R.id.switchEnableSchedule)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val rgScanType = findViewById<android.widget.RadioGroup>(R.id.rgScanType)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveSchedule)
        
        // Load saved preferences
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("scan_schedule_enabled", false)
        val hour = prefs.getInt("scan_schedule_hour", 2)
        val minute = prefs.getInt("scan_schedule_minute", 0)
        val scanType = prefs.getString("scan_schedule_type", "quick") // quick or deep

        switchEnable.isChecked = isEnabled
        timePicker.hour = hour
        timePicker.minute = minute
        if (scanType == "deep") {
            rgScanType.check(R.id.rbDeepScan)
        } else {
            rgScanType.check(R.id.rbQuickScan)
        }
        
        updateUI(isEnabled)

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            updateUI(isChecked)
        }

        btnSave.setOnClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute
            val enabled = switchEnable.isChecked
            val selectedType = if (rgScanType.checkedRadioButtonId == R.id.rbDeepScan) "deep" else "quick"

            prefs.edit().apply {
                putBoolean("scan_schedule_enabled", enabled)
                putInt("scan_schedule_hour", selectedHour)
                putInt("scan_schedule_minute", selectedMinute)
                putString("scan_schedule_type", selectedType)
                apply()
            }

            if (enabled) {
                scheduleScan(selectedHour, selectedMinute, selectedType)
                Toast.makeText(this, "Daily $selectedType scan scheduled for ${String.format("%02d:%02d", selectedHour, selectedMinute)}", Toast.LENGTH_SHORT).show()
            } else {
                cancelScan()
                Toast.makeText(this, "Scheduled scan disabled", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
    
    private fun updateUI(isEnabled: Boolean) {
        val pickerContainer = findViewById<android.view.View>(R.id.llTimePickerContainer)
        val tvStatus = findViewById<android.widget.TextView>(R.id.tvStatusFeedback)
        
        pickerContainer.visibility = if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE
        
        if (isEnabled) {
             tvStatus.text = "Scan scheduled daily"
             tvStatus.setTextColor(getColor(R.color.vigilant_green))
        } else {
             tvStatus.text = "Automatic scanning is off"
             tvStatus.setTextColor(getColor(R.color.vigilant_text_secondary))
        }
    }
    
    private fun scheduleScan(hour: Int, minute: Int, scanType: String) {
        val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(this, ScanReceiver::class.java)
        intent.putExtra("SCAN_TYPE", scanType)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, 
            0, 
            intent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }
        
        // If time has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        alarmManager.setInexactRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    
    private fun cancelScan() {
        val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(this, ScanReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, 
            0, 
            intent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
}
