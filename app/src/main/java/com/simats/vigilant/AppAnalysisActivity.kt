package com.simats.vigilant

import android.os.Bundle
import com.simats.vigilant.databinding.ActivityAppAnalysisBinding
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class AppAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppAnalysisBinding
    private var currentApp: InstalledAppRepository.InstalledApp? = null
    private var installTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val packageName = intent.getStringExtra("PACKAGE_NAME")
        
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        if (packageName != null) {
            fetchAppDetails(packageName)
        } else {
            Toast.makeText(this, "Invalid Package Name", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        setupActions(packageName)
    }

    private fun fetchAppDetails(packageName: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Real-time calculation using local PackageInfo
                val pm = packageManager
                val pkgInfo = pm.getPackageInfo(packageName, android.content.pm.PackageManager.GET_PERMISSIONS or android.content.pm.PackageManager.GET_META_DATA)
                val appName = pkgInfo.applicationInfo.loadLabel(pm).toString()
                installTime = pkgInfo.firstInstallTime
                
                // Calculate FRESH Score
                val riskResult = RiskScoreManager.calculateRiskScore(pkgInfo, pm)
                
                val topFactor = riskResult.riskFactors.maxByOrNull { it.score }
                val riskDesc = topFactor?.description ?: "No suspicious behavior detected"
                
                val app = InstalledAppRepository.InstalledApp(
                    packageName = packageName,
                    appName = appName,
                    riskScore = riskResult.totalScore,
                    riskDescription = riskDesc,
                    riskFactors = riskResult.riskFactors
                )
                currentApp = app
                        
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    setupUI(app)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                     Toast.makeText(this@AppAnalysisActivity, "App not found or uninstalled", Toast.LENGTH_SHORT).show()
                     finish()
                }
            }
        }
    }

    private fun setupUI(app: InstalledAppRepository.InstalledApp) {
        binding.topAppBar.title = app.appName
        
        // Header
        binding.tvAppName.text = app.appName
        binding.tvPackageName.text = "Version 1.0 • ${app.packageName}"
        
        try {
            binding.ivAppIcon.setImageDrawable(
                packageManager.getApplicationIcon(app.packageName)
            )
        } catch (e: Exception) {}

        // Risk Score
        binding.tvRiskScore.text = app.riskScore.toString()
        binding.tvRiskTitle.text = "${RiskScoreManager.RiskLevel.values().find { 
             it.ordinal == (if (app.riskScore <= 20) 0 else if (app.riskScore <= 40) 1 else if (app.riskScore <= 60) 2 else if (app.riskScore <= 80) 3 else 4)
        }?.label } Risk Detected" 
        
        // Just simplified mapping for display title
        val level = when {
            app.riskScore <= 20 -> RiskScoreManager.RiskLevel.SAFE
            app.riskScore <= 40 -> RiskScoreManager.RiskLevel.LOW
            app.riskScore <= 60 -> RiskScoreManager.RiskLevel.MEDIUM
            app.riskScore <= 80 -> RiskScoreManager.RiskLevel.HIGH
            else -> RiskScoreManager.RiskLevel.CRITICAL
        }
        
        // Colors & Text
        val (colorRes, bgRes, iconRes, footerText) = when (level) {
            RiskScoreManager.RiskLevel.SAFE -> Quad(
                R.color.vigilant_green, 
                R.color.vigilant_green_bg_light,
                R.drawable.ic_check_circle_green,
                "This app is safe to use."
            )
            RiskScoreManager.RiskLevel.LOW -> Quad(
                R.color.vigilant_green, 
                R.color.vigilant_green_bg_light,
                R.drawable.ic_info_outline, // or check
                "This app poses minimal risk."
            )
            RiskScoreManager.RiskLevel.MEDIUM -> Quad(
                R.color.vigilant_amber, 
                R.color.vigilant_amber_bg_light,
                R.drawable.ic_shield_alert_outline_red, // Should be amber if avail
                "This app poses a potential privacy risk."
            )
            RiskScoreManager.RiskLevel.HIGH -> Quad(
                R.color.vigilant_red, 
                R.color.vigilant_red_bg_light,
                R.drawable.ic_shield_alert_outline_red,
                "This app poses a serious privacy threat."
            )
            RiskScoreManager.RiskLevel.CRITICAL -> Quad(
                R.color.vigilant_red, 
                R.color.vigilant_red_bg_light,
                R.drawable.ic_alert_red,
                "Critical security risk detected!"
            )
        }
        
        val color = androidx.core.content.ContextCompat.getColor(this, colorRes)
        
        // 1. Text Colors
        binding.tvRiskTitle.apply {
            text = "Detected : ${level.label} Risk"
            setTextColor(color)
        }
        binding.tvRiskDesc.text = app.riskDescription
        binding.tvPrivacyThreat.text = footerText
        
        // 2. Status Pill
        binding.tvStatusLabel.text = level.label
        binding.tvStatusLabel.setTextColor(color)
        
        binding.llStatusPill.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, bgRes)
        
        binding.ivStatusIcon.setImageResource(iconRes)
        binding.ivStatusIcon.setColorFilter(color) // Tint icon to match text
        
        // 3. Risk Ring
        // We use backgroundTint on the view which has the shape drawable
        binding.vRiskRing.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(this, colorRes)

        
        // Load RecyclerView
        binding.rvRiskFactors.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvRiskFactors.adapter = RiskFactorAdapter(app.riskFactors)
    }
    
    private fun setupActions(pkgName: String?) {
        binding.topAppBar.inflateMenu(R.menu.menu_app_analysis)
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_report -> {
                    val intent = android.content.Intent(this, ReportAppActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        binding.btnViewTimeline.setOnClickListener {
             val intent = android.content.Intent(this, PermissionTimelineActivity::class.java)
             intent.putExtra("PACKAGE_NAME", pkgName)
             startActivity(intent)
        }

        binding.btnVigilantAssistant.setOnClickListener {
             val intent = android.content.Intent(this, VigilantAssistantActivity::class.java)
             currentApp?.let { app ->
                 intent.putExtra("APP_NAME", app.appName)
                 intent.putExtra("PACKAGE_NAME", app.packageName)
                 intent.putExtra("RISK_SCORE", app.riskScore)
                 intent.putExtra("RISK_LEVEL", mapRiskScoreToLevel(app.riskScore).label)
                 intent.putExtra("INSTALL_AGE", installTime)
                 intent.putStringArrayListExtra("RISK_FACTORS", ArrayList(app.riskFactors.map { it.description }))
             }
             startActivity(intent)
        }
        
        binding.btnRevoke.setOnClickListener {
             if (pkgName != null) {
                 val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                 intent.data = android.net.Uri.parse("package:$pkgName")
                 startActivity(intent)
             }
        }
        
        binding.btnUninstall.setOnClickListener {
             if (pkgName != null) {
                 try {
                     val intent = android.content.Intent(android.content.Intent.ACTION_DELETE)
                     intent.data = android.net.Uri.fromParts("package", pkgName, null)
                     startActivity(intent)
                 } catch (e: Exception) {
                     Toast.makeText(this, "Could not launch uninstall: ${e.message}", Toast.LENGTH_SHORT).show()
                     e.printStackTrace()
                 }
             } else {
                 Toast.makeText(this, "Error: Unknown package name", Toast.LENGTH_SHORT).show()
             }
        }
    }
    
    data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun mapRiskScoreToLevel(score: Int): RiskScoreManager.RiskLevel {
        return when {
            score <= 20 -> RiskScoreManager.RiskLevel.SAFE
            score <= 40 -> RiskScoreManager.RiskLevel.LOW
            score <= 60 -> RiskScoreManager.RiskLevel.MEDIUM
            score <= 80 -> RiskScoreManager.RiskLevel.HIGH
            else -> RiskScoreManager.RiskLevel.CRITICAL
        }
    }
}
