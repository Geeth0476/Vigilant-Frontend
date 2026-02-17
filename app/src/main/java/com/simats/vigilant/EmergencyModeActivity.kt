package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class EmergencyModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_mode)
        
        setupToolbar()
        setupActions()
    }
    
    private fun setupToolbar() {
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_close -> {
                    finish()
                    true
                }
                else -> false
            }
        }
        
        findViewById<MaterialButton>(R.id.btnExitEmergency).setOnClickListener {
             finish()
        }
        
        findViewById<MaterialButton>(R.id.btnViewAffected).setOnClickListener {
             val intent = Intent(this, InstalledAppsActivity::class.java)
             startActivity(intent)
        }
    }
    
    private fun setupActions() {
        // 1. Disable Sensitive (Permissions)
        findViewById<android.view.View>(R.id.cardDisableSensors).setOnClickListener {
            val intent = Intent(this, ManagePermissionsActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Revoke critical permissions here", Toast.LENGTH_LONG).show()
        }
        
        // 2. Stop Background (Kill Processes)
        findViewById<android.view.View>(R.id.cardStopBackground).setOnClickListener {
             performKillBackgroundProcesses()
        }
        
        // 3. Silent Mode (DND)
        findViewById<android.view.View>(R.id.cardSilentMode).setOnClickListener {
             openSoundSettings()
        }
        
        // 4. Safe Settings (Security)
        findViewById<android.view.View>(R.id.cardSafeSettings).setOnClickListener {
             openSecuritySettings()
        }
    }
    
    private fun performKillBackgroundProcesses() {
        val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val pm = packageManager
        val packages = pm.getInstalledPackages(0)
        
        var count = 0
        for (pkg in packages) {
            // Don't kill self or system
            if (pkg.packageName == packageName) continue
            
            // Heuristic checking for user apps
            val appInfo = pkg.applicationInfo
            if (appInfo != null && (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                 am.killBackgroundProcesses(pkg.packageName)
                 count++
            }
        }
        Toast.makeText(this, "Attempted to stop $count background apps", Toast.LENGTH_SHORT).show()
    }
    
    private fun openSoundSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Sound Settings", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openSecuritySettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
        } catch (e: Exception) {
             // Fallback
             startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }
}
