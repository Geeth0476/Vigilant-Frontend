package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.simats.vigilant.ui.dashboard.DashboardActivity

class ScanDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_dashboard)

        // Simple pulse animation for effect
        val ripple1 = findViewById<android.view.View>(R.id.ripple1)
        
        ripple1.animate().scaleX(1.2f).scaleY(1.2f).alpha(0f).setDuration(1000).withEndAction {
            ripple1.scaleX = 1f
            ripple1.scaleY = 1f
            ripple1.alpha = 0.3f
        }.start()

        // Simulate scanning delay then save results and navigate
        // Real Scan implementation (Simplified for Dashboard fast scan)
        Thread {
            val pm = packageManager
            val allPackages = pm.getInstalledPackages(android.content.pm.PackageManager.GET_META_DATA)
            val userApps = allPackages.filter { 
                val appInfo = it.applicationInfo
                if (appInfo != null) {
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) ||
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
                } else {
                    false
                }
            }
            
            // Clear and repopulate
            InstalledAppRepository.clear()
            
            userApps.forEach { packageInfo ->
                val appInfo = packageInfo.applicationInfo
                val appName = appInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName
                val pkgName = packageInfo.packageName
                
                // Calculate Risk
                val riskResult = RiskScoreManager.calculateRiskScore(packageInfo, pm)
                val topFactor = riskResult.riskFactors.maxByOrNull { it.score }
                val riskDesc = topFactor?.description ?: "No suspicious behavior detected"
                
                InstalledAppRepository.addApp(
                    InstalledAppRepository.InstalledApp(
                        packageName = pkgName,
                        appName = appName,
                        riskScore = riskResult.totalScore,
                        riskDescription = riskDesc,
                        riskFactors = riskResult.riskFactors
                    )
                )
            }
            
            // Wait a bit if scan was too fast, to show the animation
            try { Thread.sleep(2000) } catch (_: Exception) {}
            
            Handler(Looper.getMainLooper()).post {
                 // Logic to route
                 val apps = InstalledAppRepository.installedApps
                 val avgRisk = if (apps.isNotEmpty()) apps.map { it.riskScore }.average().toInt() else 0
                 
                 ScanDataManager.saveScanResult(this, avgRisk, if(avgRisk > 50) ScanDataManager.RiskLevel.HIGH else ScanDataManager.RiskLevel.SAFE)
                 
                 val intent = if (avgRisk > 50) {
                     Intent(this, DashboardActivity::class.java)
                 } else {
                     Intent(this, DashboardActivity::class.java)
                 }
                 intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                 startActivity(intent)
                 finish()
            }
        }.start()
    }
}
