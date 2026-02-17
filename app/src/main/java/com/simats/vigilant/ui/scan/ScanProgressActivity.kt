package com.simats.vigilant.ui.scan

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.simats.vigilant.databinding.ActivityScanProgressBinding
import com.simats.vigilant.databinding.ActivityScanProgressSimpleBinding
import com.simats.vigilant.R
import com.simats.vigilant.InstalledAppRepository
import com.simats.vigilant.RiskScoreManager
import com.simats.vigilant.SecurityAlert
import com.simats.vigilant.VigilantApplication
import com.simats.vigilant.data.ScanLocalDataSource
import com.simats.vigilant.ui.scan.ScanResultsActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ScanProgressActivity : AppCompatActivity() {

    private var bindingDeep: ActivityScanProgressBinding? = null
    private var bindingSimple: ActivityScanProgressSimpleBinding? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var progressStatus = 0
    private var isDeepScan = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        isDeepScan = intent.getBooleanExtra("IS_DEEP_SCAN", false)
        
        if (isDeepScan) {
            bindingDeep = ActivityScanProgressBinding.inflate(layoutInflater)
            setContentView(bindingDeep!!.root)
            
            bindingDeep!!.tvScanTitle.text = "Scanning Your Device"
            bindingDeep!!.tvScanDesc.text = "Vigilant is analyzing your device for spyware\nand suspicious behavior."
        } else {
            // Quick Scan - Simple UI
            bindingSimple = ActivityScanProgressSimpleBinding.inflate(layoutInflater)
            setContentView(bindingSimple!!.root)
            // No percentage text in simple UI per design
        }
        
        startScanSimulation(isDeepScan)
    }

    private fun startScanSimulation(isDeepScan: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val context = applicationContext
            var scanId: Int? = null
            
            // 1. Start Scan on Backend
            try {
                val api = com.simats.vigilant.data.api.ApiClient.getService(context)
                val deviceId = com.simats.vigilant.data.DeviceIdManager.getDeviceId(context)
                val mode = if (isDeepScan) "deep" else "quick"
                val response = api.startScan(mapOf("device_id" to deviceId, "mode" to mode))
                if (response.isSuccessful && response.body()?.success == true) {
                     val data = response.body()?.data
                     if (data != null) {
                         scanId = data.scan_id.toIntOrNull() // Backend sends ID
                     }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val pm = packageManager
            val allPackages = pm.getInstalledPackages(android.content.pm.PackageManager.GET_PERMISSIONS or android.content.pm.PackageManager.GET_META_DATA)
            
            val userApps = allPackages.filter { 
                val appInfo = it.applicationInfo
                if (appInfo != null) {
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) ||
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
                } else {
                    false
                }
            }

            val totalApps = userApps.size
            
            var maxRiskScore = 0
            var scoreSum = 0
            var topRiskAppPackage: String? = null
            val scannedAppsList = mutableListOf<Map<String, Any>>() // For Backend
            val highRiskApps = mutableListOf<InstalledAppRepository.InstalledApp>()
            
            InstalledAppRepository.clear()
            
            val delayTime = if (isDeepScan) 15L else 5L

            userApps.forEachIndexed { index, packageInfo ->
                progressStatus = ((index.toFloat() / totalApps.toFloat()) * 100).toInt()
                
                val appInfo = packageInfo.applicationInfo
                val appName = appInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName
                val pkgName = packageInfo.packageName
                
                // Calculate Risk
                val riskResult = RiskScoreManager.calculateRiskScore(packageInfo, pm)
                
                scoreSum += riskResult.totalScore
                if (riskResult.totalScore > maxRiskScore) {
                    maxRiskScore = riskResult.totalScore
                    topRiskAppPackage = pkgName
                }
                
                val topFactor = riskResult.riskFactors.maxByOrNull { it.score }
                val riskDesc = topFactor?.description ?: "No suspicious behavior detected"

                val installedApp = InstalledAppRepository.InstalledApp(
                    packageName = pkgName,
                    appName = appName,
                    riskScore = riskResult.totalScore,
                    riskDescription = riskDesc,
                    riskFactors = riskResult.riskFactors
                )
                
                InstalledAppRepository.addApp(installedApp)
                
                if (riskResult.totalScore >= 40) {
                    highRiskApps.add(installedApp)
                }
                
                // Add to Backend Payload
                scannedAppsList.add(mapOf(
                    "package_name" to pkgName,
                    "app_name" to appName,
                    "risk_score" to riskResult.totalScore,
                    "risk_factors" to riskResult.riskFactors.map { it.description }
                ))

                // Throttle UI updates to prevent main thread blocking (every 5 apps)
                if (index % 5 == 0 || index == totalApps - 1) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                         if (!isFinishing && !isDestroyed) {
                            updateProgressUI(progressStatus)
                            if (isDeepScan) {
                                updateStatusCards(progressStatus, appName)
                            }
                        }
                    }
                }
                
                try { Thread.sleep(delayTime) } catch (_: Exception) {}

                // Send Progress to Backend (Real-time)
                if (scanId != null && (index % 5 == 0 || index == totalApps - 1)) {
                    val currentScanned = index + 1
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                            val progressMap = mapOf(
                                "apps_scanned" to currentScanned,
                                "total_apps" to totalApps
                            )
                            api.updateScanProgress(scanId.toString(), progressMap)
                        } catch(e: Exception) {
                            // Silent fail for progress updates
                        }
                    }
                }
            }
            
            val avgScore = if (totalApps > 0) scoreSum / totalApps else 0
            val repository = (application as VigilantApplication).scanRepository
            val riskLevel = repository.getRiskLevelFromScore(avgScore)
            
            // Save Locally
            repository.saveScanResult(avgScore, riskLevel)
            repository.saveTopRiskApp(topRiskAppPackage)
            repository.saveAlerts(highRiskApps.map { app -> 
                SecurityAlert.createAlertFromApp(app.packageName, app.appName, app.riskScore, app.riskFactors.map { it.description })
            }.take(3))
            
            // 2. Complete Scan on Backend
            if (scanId != null) {
                try {
                    val levelStr = when(riskLevel) {
                        ScanLocalDataSource.RiskLevel.HIGH -> "HIGH"
                        ScanLocalDataSource.RiskLevel.MEDIUM -> "MEDIUM"
                        else -> "SAFE"
                    }
                    
                    val payload = mapOf(
                        "scan_id" to scanId!!,
                        "risk_score" to avgScore,
                        "risk_level" to levelStr,
                        "apps" to scannedAppsList
                    )
                    
                    val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                    val response = api.completeScan(payload)
                    
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("ScanError", "Backend rejected scan complete: $errorBody")
                        // Optional: Show error to user via Toast on main thread if critical
                    } else {
                        android.util.Log.d("ScanSuccess", "Database updated successfully")
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("ScanError", "Network failed during completion: ${e.message}")
                }
            }
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    updateProgressUI(100)
                    navigateToNextScreen()
                }
            }
        }
    }
    
    private fun updateProgressUI(progress: Int) {
        if (isDeepScan) {
            bindingDeep?.progressIndicator?.progress = progress
            bindingDeep?.tvProgressPercentage?.text = "$progress%"
        } else {
            bindingSimple?.progressSimple?.progress = progress
        }
    }
    
    private fun updateStatusCards(progress: Int, currentAppName: String) {
        // This only runs in Deep Scan mode where these views exist
        val binding = bindingDeep ?: return // Safety check
        
        binding.tvStatusApps.text = "Scanning: $currentAppName"

        if (progress > 50) {
             binding.tvStatusPerms.text = getString(R.string.scan_status_analyzing)
             binding.tvStatusPerms.setTextColor(getColor(R.color.vigilant_blue))
             binding.progressPerms.visibility = View.VISIBLE
             binding.ivStatusPerms.visibility = View.GONE
        }
        
        if (progress > 80) {
             binding.tvStatusBg.text = getString(R.string.scan_status_analyzing)
             binding.tvStatusBg.setTextColor(getColor(R.color.vigilant_blue))
             binding.ivStatusBg.visibility = View.GONE
        }
    }

    private fun navigateToNextScreen() {
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                val targetIntent = Intent(this, ScanResultActivity::class.java)
                // We saved to repo, so dashboard will update. 
                // ScanResultsActivity might just show list.
                // But passing Score might be useful if ScanResultsActivity uses it.
                // For now just navigate.
                targetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(targetIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
        }, 500)
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down)
    }
}
