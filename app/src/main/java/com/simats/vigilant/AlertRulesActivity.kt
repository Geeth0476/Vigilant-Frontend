package com.simats.vigilant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class AlertRulesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_rules)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        setupSwitch(
            R.id.alertCritical, 
            R.drawable.ic_alert_red, 
            getString(R.string.chk_critical), 
            "Receive alerts for critical threats", 
            "pref_alert_crit", 
            true
        )
        
        setupSwitch(
            R.id.alertSuspicious, 
            R.drawable.ic_info_warning_yellow, 
            getString(R.string.chk_suspicious), 
            "Receive alerts for suspicious apps", 
            "pref_alert_susp", 
            true
        )
        
        setupSwitch(
            R.id.alertPermission, 
            R.drawable.ic_lock_blue, 
            getString(R.string.chk_permission), 
            "Receive alerts for permission changes", 
            "pref_alert_perm", 
            true
        )
        
        setupSwitch(
            R.id.alertQuietHours, 
            R.drawable.ic_bell_off_blue, 
            getString(R.string.opt_quiet_hours), 
            "Mute alerts during night time", 
            "pref_quiet_hours", 
            false
        )
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
