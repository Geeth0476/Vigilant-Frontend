package com.simats.vigilant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ScanSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_settings)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        // Fetch valid settings from backend first
        fetchSettings()

        // Scan Settings
        setupSwitch(
            R.id.optAutoScan, 
            R.drawable.ic_scan_blue, 
            getString(R.string.opt_auto_scan), 
            getString(R.string.desc_auto_scan), 
            "pref_auto_scan", 
            "auto_scan", // Backend Key
            true
        )
        
        setupSwitch(
            R.id.optDeepScan, 
            R.drawable.ic_eye_purple, 
            getString(R.string.opt_deep_scan), 
            getString(R.string.desc_deep_scan), 
            "pref_deep_scan", 
            "deep_scan", // Backend Key
            false
        )
        
        setupNavigationRow(
            R.id.optScheduledScan,
            R.drawable.ic_schedule,
            "Scheduled Scan",
            "Configure daily automatic scans"
        ) {
            startActivity(android.content.Intent(this, ScheduleScanActivity::class.java))
        }
    }
    
    private fun fetchSettings() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                val response = api.getScanSettings()
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                        val editor = prefs.edit()
                        
                        // Check keys exist in map
                        if (data.containsKey("auto_scan")) {
                            editor.putBoolean("pref_auto_scan", (data["auto_scan"] as? Boolean) == true)
                        }
                        if (data.containsKey("deep_scan")) {
                            editor.putBoolean("pref_deep_scan", (data["deep_scan"] as? Boolean) == true)
                        }
                        editor.apply()
                        
                        // Refresh UI
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                             refreshSwitches()
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun refreshSwitches() {
        // Re-bind switch values from prefs
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        
        findViewById<View>(R.id.optAutoScan).findViewById<SwitchMaterial>(R.id.switchSetting).isChecked = 
            prefs.getBoolean("pref_auto_scan", true)
            
        findViewById<View>(R.id.optDeepScan).findViewById<SwitchMaterial>(R.id.switchSetting).isChecked = 
            prefs.getBoolean("pref_deep_scan", false)
    }

    private fun setupNavigationRow(includeId: Int, iconRes: Int, title: String, desc: String, onClick: () -> Unit) {
        val view = findViewById<View>(includeId)
        view.findViewById<ImageView>(R.id.iconSetting).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvSettingTitle).text = title
        view.findViewById<TextView>(R.id.tvSettingDesc).text = desc
        view.setOnClickListener { onClick() }
    }

    private fun setupSwitch(includeId: Int, iconRes: Int, title: String, desc: String, key: String, backendKey: String, defValue: Boolean) {
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        val view = findViewById<View>(includeId)
        
        view.findViewById<ImageView>(R.id.iconSetting).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvSettingTitle).text = title
        view.findViewById<TextView>(R.id.tvSettingDesc).text = desc
        
        val switchView = view.findViewById<SwitchMaterial>(R.id.switchSetting)
        switchView.isChecked = prefs.getBoolean(key, defValue)
        
        switchView.setOnCheckedChangeListener { _, isChecked ->
            // 1. Save Local
            prefs.edit().putBoolean(key, isChecked).apply()
            
            // 2. Push to Backend
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                    val map = mapOf(backendKey to isChecked) // Send partial update
                    api.updateScanSettings(map)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }
}
