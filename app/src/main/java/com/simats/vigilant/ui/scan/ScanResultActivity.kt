package com.simats.vigilant.ui.scan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.simats.vigilant.databinding.ActivityScanResultBinding
import com.simats.vigilant.R
import com.simats.vigilant.ui.dashboard.DashboardActivity
import com.simats.vigilant.InstalledAppRepository
import com.simats.vigilant.ScanDataManager

class ScanResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val riskScore = intent.getIntExtra("RISK_SCORE", 0)
        // Fallback to ScanDataManager if intent extra missing? No, intent should have it.
        // Or re-fetch? Let's use re-fetch as backup.
        val score = if (riskScore == 0) ScanDataManager.getRiskScore(this) else riskScore
        val riskLevel = ScanDataManager.getRiskLevelFromScore(score)
        
        setupUI(score, riskLevel)
    }

    private fun setupUI(score: Int, level: ScanDataManager.RiskLevel) {
        // 1. Score & Progress
        binding.tvRiskScore.text = score.toString()
        binding.progressRisk.setProgressCompat(score, true)
        
        // 2. Theme & Content based on Level
        when (level) {
            ScanDataManager.RiskLevel.HIGH -> setupHighRiskUI()
            ScanDataManager.RiskLevel.MEDIUM -> setupMediumRiskUI()
            ScanDataManager.RiskLevel.SAFE -> setupSafeUI() // Low/Safe
        }

        // 3. Buttons
        binding.btnDashboard.setOnClickListener {
             val intent = Intent(this, DashboardActivity::class.java)
             intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
             startActivity(intent)
             overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        binding.btnDetails.setOnClickListener {
             // In real app, goes to results list or specific details
             // For now, scan results list or toast
             val intent = Intent(this, ScanResultsActivity::class.java)
             startActivity(intent)
             overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun setupHighRiskUI() {
        // Colors
        val color = ContextCompat.getColor(this, R.color.vigilant_red)
        
        binding.progressRisk.setIndicatorColor(color)
        binding.tvRiskStatus.text = getString(R.string.risk_level_high)
        binding.tvRiskStatus.setTextColor(color)
        
        binding.tvRiskDesc.text = getString(R.string.risk_desc) // "Critical threats found..."
        
        // Stats
        binding.cardRiskApps.visibility = View.VISIBLE
        binding.cardWarnings.visibility = View.VISIBLE
        
        // Populate stats (Mock or Real?)
        // In a real refactor, we'd pass these counts via Intent or fetch from Repo.
        // For now, let's look at InstalledAppRepository to see counts.
        val highCount = InstalledAppRepository.installedApps.count { it.riskScore >= 70 }
        val medCount = InstalledAppRepository.installedApps.count { it.riskScore in 40..69 }
        
        // Card 1: High Risk Apps
        binding.ivStats1Icon.setImageResource(R.drawable.ic_shield_alert_outline_red)
        binding.tvStats1Count.text = highCount.toString()
        binding.tvStats1Count.setTextColor(color)
        binding.tvStats1Label.text = getString(R.string.high_risk_apps_label)
        
        // Card 2: Warnings (Medium)
        binding.ivStats2Icon.setImageResource(R.drawable.ic_warning_outline_orange)
        binding.tvStats2Count.text = medCount.toString()
        binding.tvStats2Count.setTextColor(ContextCompat.getColor(this, R.color.vigilant_orange))
        binding.tvStats2Label.text = getString(R.string.warnings_label)
        
        // Footer: Scanned info
        binding.tvScannedSubtext.text = getString(R.string.db_update_time) 
        binding.tvScannedSubtext.setTextColor(ContextCompat.getColor(this, R.color.vigilant_success_green))
        
        binding.llRecommendNote.visibility = View.GONE
    }

    private fun setupMediumRiskUI() {
        val color = ContextCompat.getColor(this, R.color.vigilant_orange) // Amber/Orange
        
        binding.progressRisk.setIndicatorColor(color)
        binding.tvRiskStatus.text = "Potential Risks"
        binding.tvRiskStatus.setTextColor(color)
        
        binding.tvRiskDesc.text = "We found some permissions that need your review."
        
        val highCount = InstalledAppRepository.installedApps.count { it.riskScore >= 70 }
        val medCount = InstalledAppRepository.installedApps.count { it.riskScore in 40..69 }

        // Card 1: Warnings (Medium) - Promoted to first card
        binding.ivStats1Icon.setImageResource(R.drawable.ic_warning_outline_orange)
        binding.tvStats1Count.text = medCount.toString()
        binding.tvStats1Count.setTextColor(color)
        binding.tvStats1Label.text = getString(R.string.warnings_label)
        
        // Card 2: Safe Apps or something else? 
        // Or keep High Risk (0)?
        binding.ivStats2Icon.setImageResource(R.drawable.ic_shield_check_green)
        binding.tvStats2Count.text = highCount.toString() // Should be 0 usually
        binding.tvStats2Count.setTextColor(ContextCompat.getColor(this, R.color.vigilant_green))
        binding.tvStats2Label.text = "Critical Threats"
        
        binding.llRecommendNote.visibility = View.GONE
    }

    private fun setupSafeUI() {
        val color = ContextCompat.getColor(this, R.color.vigilant_green) // #4CAF50
        
        binding.progressRisk.setIndicatorColor(color)
        binding.tvRiskStatus.text = getString(R.string.risk_level_low) // "You are Safe"
        binding.tvRiskStatus.setTextColor(color)
        
        binding.tvRiskDesc.text = getString(R.string.risk_desc_low) // "No threats found..."
        
        // Stats
        // Card 1: Threats (0)
        binding.ivStats1Icon.setImageResource(R.drawable.ic_shield_check_green)
        binding.tvStats1Count.text = "0"
        binding.tvStats1Count.setTextColor(color)
        binding.tvStats1Label.text = getString(R.string.high_risk_apps_label)
        
        // Card 2: Warnings (Maybe 0 or low)
        val medCount = InstalledAppRepository.installedApps.count { it.riskScore in 40..69 }
        binding.ivStats2Icon.setImageResource(R.drawable.ic_info_warning_yellow)
        binding.tvStats2Count.text = medCount.toString()
        binding.tvStats2Count.setTextColor(ContextCompat.getColor(this, R.color.vigilant_amber))
        binding.tvStats2Label.text = getString(R.string.minor_warnings_label)
        
        // Extra Note
        binding.llRecommendNote.visibility = View.VISIBLE
        binding.tvRecommendNote.text = getString(R.string.recommend_scans_note)
        
        // Update scanned info text color to match "Safe" vibe (Green)
        binding.tvScannedSubtext.text = "All systems normal"
        binding.tvScannedSubtext.setTextColor(color)
    }
}

