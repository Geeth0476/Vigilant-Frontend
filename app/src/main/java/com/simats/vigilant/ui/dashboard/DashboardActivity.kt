package com.simats.vigilant.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat
import com.simats.vigilant.BaseActivity
import com.simats.vigilant.CommunityFeedActivity
import com.simats.vigilant.EmergencyModeActivity
import com.simats.vigilant.InstalledAppsActivity
import com.simats.vigilant.R
import com.simats.vigilant.ScheduleScanActivity
import com.simats.vigilant.SecurityAlert
import com.simats.vigilant.SettingsHomeActivity
import com.simats.vigilant.AlertDetailActivity
import com.simats.vigilant.AppAnalysisActivity
import com.simats.vigilant.InstalledAppRepository
import com.simats.vigilant.ProfileAccountActivity
import com.simats.vigilant.ui.scan.ScanProgressActivity
import com.simats.vigilant.ui.scan.ScanResultsActivity
import com.simats.vigilant.databinding.ActivityDashboardBinding

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupUI()
        setupBottomNav()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        
        // Trigger animation from 0
        val currentScore = viewModel.riskScore.value ?: 0
        animateRiskScore(currentScore)
        
        // Refresh local state to capture any background updates (e.g. from PackageChangeReceiver)
        viewModel.refreshDataFromLocal()
        
        viewModel.refreshDashboardState()
    }

    private fun setupBottomNav() {
        setupBottomNavigation(binding.bottomNavigation, R.id.nav_dashboard)
    }

    private fun setupUI() {
        // Wire up buttons common to all states
        binding.cardRegularScans.setOnClickListener { startScan(false) }
        binding.cardMonitorPermissions.setOnClickListener { startActivityWithTransition(Intent(this, InstalledAppsActivity::class.java)) }
        binding.cardCheckCommunity.setOnClickListener { startActivityWithTransition(Intent(this, CommunityFeedActivity::class.java)) }
        binding.cardScheduledScan.setOnClickListener { startActivityWithTransition(Intent(this, ScheduleScanActivity::class.java)) }
        
        binding.tvViewAllAlerts.setOnClickListener {
             startActivityWithTransition(Intent(this, ScanResultsActivity::class.java))
        }

        // Header
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        val userName = prefs.getString("profile_name", "User")
        binding.topAppBar.title = "Hello, $userName"
        
        setupTopBarActions()
    }
    
    private fun setupObservers() {
        viewModel.riskScore.observe(this) { score ->
            animateRiskScore(score)
        }

        viewModel.riskLevel.observe(this) { level ->
            updateRiskUI(level)
        }

        viewModel.scanStatus.observe(this) { time ->
            // Use this time to update the "Last scan" text at bottom
            // binding.tvLastScan.text = "Last scan: $time" // Removing pending XML verification
            
            // Also update subtitle
            // If "Never", we still show the UI but maybe change text
            val currentSubtitle = binding.topAppBar.subtitle?.toString()
            if (currentSubtitle != null && !currentSubtitle.startsWith("Status")) { // Only update if not overridden
                 val prefix = currentSubtitle.substringBefore(" •")
                 binding.topAppBar.subtitle = "$prefix • $time"
            }
            if (time.contains("Never", true)) {
                 binding.topAppBar.subtitle = "Device Not Scanned"
                 // Ensure we update UI to reflect "Not Scanned" state but keep meter visible (handled by updateRiskUI mostly)
                 // But let's force update logic here if needed, or rely on updateRiskUI
                 showNeverScannedState() 
            }
        }

        viewModel.alerts.observe(this) { alerts ->
            updateAlertsUI(alerts)
        }

        viewModel.topRiskApp.observe(this) { packageName ->
            updateHighRiskCard(packageName)
        }
        
        viewModel.isProtected.observe(this) { _ ->
             // usage is optional if riskLevel covers it.
        }
        
        viewModel.activeScanProgress.observe(this) { progress ->
            if (progress != null) {
                showScanningState(progress)
            } else {
                // If scan finishes, the polling will eventually fetch the new risk score
                // and updateRiskUI will be called by the riskScore/riskLevel observers.
                // However, we might want to ensure we exit "Scanning" mode immediately if we were in it.
                // The riskLevel observer will naturally override the text/colors, 
                // but we should ensure the hero text is reset if it doesn't trigger automatically.
                // For now, let's assume riskLevel update handles the transition back to "Safe"/"High Risk"
            }
        }
    }
    
    private fun showScanningState(progress: Int) {
         binding.topAppBar.subtitle = "System Scan in Progress..."
         
         // Visuals for scanning
         binding.tvHeroTitle.text = "Scanning..."
         binding.tvHeroTitle.setTextColor(ContextCompat.getColor(this, R.color.vigilant_blue))
         binding.tvHeroDesc.text = "Checking system apps and files ($progress%)"
         
         binding.progressHeroRisk.setIndicatorColor(ContextCompat.getColor(this, R.color.vigilant_blue))
         binding.progressHeroRisk.trackColor = ContextCompat.getColor(this, R.color.vigilant_nav_pill_color)
         
         // Animate to current progress
         // Reuse existing animator or just set progress if frequent updates
         binding.progressHeroRisk.setProgress(progress, true)
         binding.tvHeroScore.text = "$progress%"
         binding.tvHeroScore.setTextColor(ContextCompat.getColor(this, R.color.vigilant_blue))

         binding.ivHeroIcon.visibility = View.GONE
         binding.llScoreContainer.visibility = View.VISIBLE
         
         // Disable scan buttons or change text?
         binding.btnActionPrimary.text = "Scanning..."
         binding.btnActionPrimary.isEnabled = false
         binding.btnActionSecondary.isEnabled = false
    }

    private fun startScan(isDeep: Boolean) {
        val intent = Intent(this, ScanProgressActivity::class.java)
        intent.putExtra("IS_DEEP_SCAN", isDeep)
        startActivityWithTransition(intent, TransitionType.SLIDE_UP)
    }

    private fun showNeverScannedState() {
         binding.topAppBar.subtitle = "Status Unknown"
         binding.tvHeroTitle.text = "Device Not Scanned"
         binding.tvHeroTitle.setTextColor(ContextCompat.getColor(this, R.color.black))
         binding.tvHeroDesc.text = "Run a scan to check for threats."
         
         binding.progressHeroRisk.setIndicatorColor(ContextCompat.getColor(this, R.color.vigilant_blue))
         binding.progressHeroRisk.trackColor = ContextCompat.getColor(this, R.color.vigilant_nav_pill_color)
         binding.progressHeroRisk.progress = 0
         binding.tvHeroScore.text = "0"
         binding.tvHeroScore.setTextColor(ContextCompat.getColor(this, R.color.vigilant_blue))
         
         binding.ivHeroIcon.visibility = View.GONE
         binding.llScoreContainer.visibility = View.VISIBLE
         binding.llHighRiskContainer.visibility = View.GONE
         
         setupStandardButtons()
    }

    private fun updateRiskUI(level: com.simats.vigilant.data.ScanLocalDataSource.RiskLevel) {
        // If never scanned, we might get SAFE default but time is "Never".
        // The lastScanTime observer handles the "Never" case. 
        // We assume valid scan here if it's not "Never".
        
        // If never scanned, we still show the risk meter (safe/0) as per user request
        val lastScanStr = viewModel.scanStatus.value ?: "Never"
        // if (lastScanStr.contains("Never", true)) return // REMOVED to show meter
        if (lastScanStr.contains("Never", true)) {
             // Let showNeverScannedState handle the text/colors, but we ensure container is visible there
             showNeverScannedState()
             return
        }

        binding.ivHeroIcon.visibility = View.GONE
        binding.llScoreContainer.visibility = View.VISIBLE
        
        when (level) {
            com.simats.vigilant.data.ScanLocalDataSource.RiskLevel.HIGH -> {
                binding.topAppBar.subtitle = "High Risk Detected • $lastScanStr"
                val color = ContextCompat.getColor(this, R.color.vigilant_red)
                
                binding.progressHeroRisk.setIndicatorColor(color)
                binding.tvHeroScore.setTextColor(color)
                
                binding.tvHeroTitle.text = getString(R.string.risk_level_high)
                binding.tvHeroTitle.setTextColor(color)
                binding.tvHeroDesc.text = getString(R.string.risk_desc)
                
                binding.llHighRiskContainer.visibility = View.VISIBLE
                
                // Configure Buttons for High Risk
                binding.btnActionPrimary.text = "Scan Now"
                binding.btnActionPrimary.setOnClickListener { startScan(false) }
                
                binding.btnActionSecondary.text = "Emergency"
                binding.btnActionSecondary.setIconResource(R.drawable.ic_lightning_red)
                binding.btnActionSecondary.setIconTintResource(R.color.vigilant_red)
                binding.btnActionSecondary.setTextColor(ContextCompat.getColor(this, R.color.vigilant_red))
                binding.btnActionSecondary.strokeColor = ContextCompat.getColorStateList(this, R.color.vigilant_red)
                binding.btnActionSecondary.setOnClickListener { startActivityWithTransition(Intent(this, EmergencyModeActivity::class.java), TransitionType.FADE) }
            }
            com.simats.vigilant.data.ScanLocalDataSource.RiskLevel.MEDIUM -> {
                binding.topAppBar.subtitle = "Potential Risks • $lastScanStr"
                val color = ContextCompat.getColor(this, R.color.vigilant_amber)
                
                binding.progressHeroRisk.setIndicatorColor(color)
                binding.tvHeroScore.setTextColor(color)
                
                binding.tvHeroTitle.text = "Potential Risks"
                binding.tvHeroTitle.setTextColor(color)
                binding.tvHeroDesc.text = "We found some privacy warnings that need review."
                
                binding.llHighRiskContainer.visibility = View.GONE
                setupStandardButtons()
            }
            com.simats.vigilant.data.ScanLocalDataSource.RiskLevel.SAFE -> {
                binding.topAppBar.subtitle = "Protected • $lastScanStr"
                val color = ContextCompat.getColor(this, R.color.vigilant_green)
                
                binding.progressHeroRisk.setIndicatorColor(color)
                binding.tvHeroScore.setTextColor(color)
                
                binding.tvHeroTitle.text = getString(R.string.all_clear_title)
                binding.tvHeroTitle.setTextColor(ContextCompat.getColor(this, R.color.black))
                binding.tvHeroDesc.text = getString(R.string.all_clear_desc)
                
                binding.llHighRiskContainer.visibility = View.GONE
                setupStandardButtons()
            }
        }
    }
    
    private fun setupStandardButtons() {
        binding.btnActionPrimary.text = "Quick Scan"
        binding.btnActionPrimary.setOnClickListener { startScan(false) }
        
        binding.btnActionSecondary.text = "Deep Scan"
        binding.btnActionSecondary.setIconResource(R.drawable.ic_search)
        binding.btnActionSecondary.setIconTintResource(R.color.vigilant_blue)
        binding.btnActionSecondary.setTextColor(ContextCompat.getColor(this, R.color.vigilant_blue))
        binding.btnActionSecondary.strokeColor = ContextCompat.getColorStateList(this, R.color.vigilant_blue)
        binding.btnActionSecondary.setOnClickListener { startScan(true) }
    }

    private fun updateHighRiskCard(topRiskPkg: String?) {
        if (topRiskPkg != null) {
            try {
                binding.llHighRiskContainer.visibility = View.VISIBLE // actually referencing the container inside
                // Wait, llHighRiskContainer is the parent Linear Layout for the card. 
                // In XML: <LinearLayout android:id="@+id/llHighRiskContainer" ...>
                
                // Oops, the visibility logic was in updateRiskUI too. 
                // Safe to set VISIBLE here if we have a package, but updateRiskUI handles the "mode".
                
                binding.cardRiskApp.visibility = View.VISIBLE
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(topRiskPkg, 0)
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                
                binding.tvRiskAppName.text = appLabel
                binding.tvRiskAppLabel.text = "High Risk Detected"
                
                binding.cardRiskApp.setOnClickListener {
                    val intent = Intent(this, InstalledAppsActivity::class.java)
                    intent.putExtra("PACKAGE_NAME", topRiskPkg)
                    startActivityWithTransition(intent)
                }
                
                // Match Logic
                val repoApp = InstalledAppRepository.installedApps.find { it.packageName == topRiskPkg }
                if (repoApp != null) {
                    binding.tvRiskAppDescription.text = repoApp.riskDescription
                    binding.tvViewAnalysis.setOnClickListener {
                        val intent = Intent(this, InstalledAppsActivity::class.java)
                        intent.putExtra("PACKAGE_NAME", topRiskPkg)
                        startActivityWithTransition(intent)
                    }
                }
            } catch (e: Exception) {
                // If app not found or error, hide
                 // Might be strictly Risk mode but no specific app found? 
                 // We keep container but maybe hide card? 
                 // For now, let's just log or ignore.
                 binding.cardRiskApp.visibility = View.GONE
            }
        } else {
             binding.cardRiskApp.visibility = View.GONE
        }
    }
    
    private fun updateAlertsUI(alerts: List<SecurityAlert>) {
        if (alerts.isNotEmpty()) {
             binding.llRecentAlerts.visibility = View.VISIBLE
             
             // Alert 1
             binding.cardAlert1.visibility = View.VISIBLE
             val a1 = alerts[0]
             binding.tvAlert1Title.text = a1.title
             binding.tvAlert1Desc.text = a1.description
             binding.tvAlert1Time.text = getTimeAgo(a1.timestamp)
             val color1 = ContextCompat.getColor(this,
                 if (a1.severity == SecurityAlert.Severity.HIGH) R.color.vigilant_red 
                 else if (a1.severity == SecurityAlert.Severity.MEDIUM) R.color.vigilant_amber 
                 else R.color.vigilant_green
             )
             binding.alert1Border.setBackgroundColor(color1)
             
             binding.cardAlert1.setOnClickListener { openAlert(a1) }
             
             // Alert 2
             if (alerts.size > 1) {
                 binding.cardAlert2.visibility = View.VISIBLE
                 val a2 = alerts[1]
                 binding.tvAlert2Title.text = a2.title
                 binding.tvAlert2Desc.text = a2.description
                 binding.tvAlert2Time.text = getTimeAgo(a2.timestamp)
                 val color2 = ContextCompat.getColor(this,
                     if (a2.severity == SecurityAlert.Severity.HIGH) R.color.vigilant_red 
                     else if (a2.severity == SecurityAlert.Severity.MEDIUM) R.color.vigilant_amber 
                     else R.color.vigilant_green
                 )
                 binding.alert2Border.setBackgroundColor(color2)
                 
                 binding.cardAlert2.setOnClickListener { openAlert(a2) }
             } else {
                 binding.cardAlert2.visibility = View.GONE
             }
             
        } else {
            binding.llRecentAlerts.visibility = View.GONE
        }
    }
    
    private fun openAlert(alert: SecurityAlert) {
         try {
             if (alert.packageName != null) {
                 val intent = Intent(this, InstalledAppsActivity::class.java)
                 intent.putExtra("PACKAGE_NAME", alert.packageName)
                 startActivityWithTransition(intent)
             } else {
                 val intent = Intent(this, AlertDetailActivity::class.java)
                 intent.putExtra("alert", alert)
                 startActivityWithTransition(intent)
             }
         } catch (e: Exception) {
             Toast.makeText(this, "Unable to open alert details", Toast.LENGTH_SHORT).show()
         }
    }

    private fun setupTopBarActions() {
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivityWithTransition(Intent(this, SettingsHomeActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private var currentAnimator: android.animation.ValueAnimator? = null

    private fun animateRiskScore(score: Int) {
        // Cancel previous animation if running
        currentAnimator?.cancel()

        // Create new animator from 0 to target score
        val animator = android.animation.ValueAnimator.ofInt(0, score)
        animator.duration = resources.getInteger(R.integer.anim_duration_score_count).toLong()
        animator.interpolator = android.view.animation.DecelerateInterpolator()

        // Pre-calculate colors
        val green = ContextCompat.getColor(this, R.color.vigilant_green)
        val amber = ContextCompat.getColor(this, R.color.vigilant_amber)
        val red = ContextCompat.getColor(this, R.color.vigilant_red)
        val argbEvaluator = android.animation.ArgbEvaluator()

        animator.addUpdateListener { animation ->
            val currentValue = animation.animatedValue as Int
            // Update Text
            binding.tvHeroScore.text = currentValue.toString()
            // Update Progress
            binding.progressHeroRisk.progress = currentValue
            
            // Calculate Color based on current value (0-100)
            val color = when {
                currentValue <= 40 -> green
                currentValue <= 70 -> argbEvaluator.evaluate(
                    (currentValue - 40) / 30f, 
                    green, 
                    amber
                ) as Int
                else -> argbEvaluator.evaluate(
                    (currentValue - 70) / 30f, 
                    amber, 
                    red
                ) as Int
            }
            
            // Apply Dynamic Color
            binding.tvHeroScore.setTextColor(color)
            binding.progressHeroRisk.setIndicatorColor(color)
        }

        currentAnimator = animator
        animator.start()
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        val hours = minutes / 60
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${hours / 24}d ago"
        }
    }
}

