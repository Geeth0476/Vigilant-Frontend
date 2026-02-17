package com.simats.vigilant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class PrivacyDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_data)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        setupSwitch(
            R.id.optUsageStats, 
            R.drawable.ic_document_grey, 
            getString(R.string.opt_usage_stats), 
            getString(R.string.desc_usage_stats), 
            "pref_usage_stats", 
            true
        )
        
        setupSwitch(
            R.id.optCrashReports, 
            R.drawable.ic_info_warning_yellow, 
            getString(R.string.opt_crash_reports), 
            getString(R.string.desc_crash_reports), 
            "pref_crash_reports", 
            true
        )
        
        findViewById<View>(R.id.btnDownloadData).setOnClickListener {
             Toast.makeText(this, "Preparing data export... This may take a minute.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSwitch(includeId: Int, iconRes: Int, title: String, desc: String, key: String, defValue: Boolean) {
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        val view = findViewById<View>(includeId)
        
        view.findViewById<ImageView>(R.id.iconSetting).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.tvSettingTitle).text = title
        view.findViewById<TextView>(R.id.tvSettingDesc).text = desc
        
        val switchView = view.findViewById<SwitchMaterial>(R.id.switchSetting)
        switchView.isChecked = prefs.getBoolean(key, defValue)
        
        switchView.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(key, isChecked).apply()
        }
    }
}
