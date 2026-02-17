package com.simats.vigilant.ui.scan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.vigilant.databinding.ActivityScanResultsBinding
import com.simats.vigilant.BaseActivity
import com.simats.vigilant.R
import com.simats.vigilant.InstalledAppsActivity
import com.simats.vigilant.SettingsHomeActivity
import com.simats.vigilant.InstalledAppRepository
import com.simats.vigilant.AppAnalysisActivity

class ScanResultsActivity : BaseActivity() {

    private lateinit var binding: ActivityScanResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivityWithTransition(Intent(this, SettingsHomeActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Initialize repo-based data
        loadRepoData()
        
        setupBottomNavigation(binding.bottomNavigation, R.id.nav_dashboard)
        setupToolbar(binding.topAppBar) // Uses default true for homeAsUp
        
        binding.btnViewApps.setOnClickListener {
             startActivityWithTransition(Intent(this, InstalledAppsActivity::class.java))
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadRepoData()
    }
    
    private fun loadRepoData() {
        val apps = InstalledAppRepository.installedApps
        val highRiskCount = apps.count { it.riskScore > 60 }
        
        // Hero Ring
        binding.tvHeroCount.text = highRiskCount.toString()
        
        binding.btnReviewThreats.setOnClickListener {
             // Go to Detail Analysis of the first High/Med threat
             val threat = apps.find { it.riskScore > 60 } ?: apps.find { it.riskScore > 20 }
             if (threat != null) {
                 val intent = Intent(this, AppAnalysisActivity::class.java)
                 intent.putExtra("PACKAGE_NAME", threat.packageName)
                  startActivityWithTransition(intent)
             } else {
                 showError("No significant threats found to review.")
             }
        }
    }
}
