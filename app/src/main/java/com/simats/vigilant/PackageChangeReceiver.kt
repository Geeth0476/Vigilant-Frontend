package com.simats.vigilant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.simats.vigilant.data.ScanLocalDataSource
import com.simats.vigilant.data.repository.ScanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_PACKAGE_ADDED || 
            action == Intent.ACTION_PACKAGE_REMOVED || 
            action == Intent.ACTION_PACKAGE_REPLACED ||
            action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {

            // Run a silent background scan to update risk score
            updateRiskScore(context)
        }
    }

    private fun updateRiskScore(context: Context) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        
        scope.launch {
            try {
                val pm = context.packageManager
                val allPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA)
                
                // Filter User Apps
                val userApps = allPackages.filter { 
                    val appInfo = it.applicationInfo
                    if (appInfo != null) {
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) ||
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
                    } else {
                        false
                    }
                }

                var scoreSum = 0
                var maxRiskScore = 0
                var topRiskAppPackage: String? = null
                val highRiskApps = mutableListOf<InstalledAppRepository.InstalledApp>()

                val totalApps = userApps.size

                userApps.forEach { packageInfo ->
                    val riskResult = RiskScoreManager.calculateRiskScore(packageInfo, pm)
                    scoreSum += riskResult.totalScore

                    if (riskResult.totalScore > maxRiskScore) {
                        maxRiskScore = riskResult.totalScore
                        topRiskAppPackage = packageInfo.packageName
                    }
                    
                    if (riskResult.totalScore >= 40) {
                         val appName = packageInfo.applicationInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName
                         val topFactor = riskResult.riskFactors.maxByOrNull { it.score }
                         val riskDesc = topFactor?.description ?: "No suspicious behavior detected"
                         
                         highRiskApps.add(InstalledAppRepository.InstalledApp(
                            packageInfo.packageName,
                            appName,
                            riskResult.totalScore,
                            riskDesc,
                            riskResult.riskFactors
                         ))
                    }
                }

                val avgScore = if (totalApps > 0) scoreSum / totalApps else 0
                
                // We need access to Repository or DataSource. 
                val localSource = ScanLocalDataSource(context)
                
                // Determine Level
                val riskLevel = when {
                    avgScore <= 20 -> ScanLocalDataSource.RiskLevel.SAFE
                    avgScore <= 70 -> ScanLocalDataSource.RiskLevel.MEDIUM
                    else -> ScanLocalDataSource.RiskLevel.HIGH
                }

                // Save Results
                localSource.saveScanResult(avgScore, riskLevel)
                localSource.saveTopRiskApp(topRiskAppPackage)
                
                // Save Alerts (Persist them)
                val alerts = highRiskApps.map { app -> 
                    SecurityAlert.createAlertFromApp(app.packageName, app.appName, app.riskScore, app.riskFactors.map { it.description })
                }.take(5) // Limit to top 5
                
                localSource.saveAlerts(alerts)
                
                // Also update InstalledAppRepository in-memory cache if the app is running
                InstalledAppRepository.populate(context)

                // --- Real-time Notification for High Risk ---
                if (riskLevel == ScanLocalDataSource.RiskLevel.HIGH && highRiskApps.isNotEmpty()) {
                    showRiskNotification(context, highRiskApps.size)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showRiskNotification(context: Context, appCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "vigilant_risk_alert"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Security Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.simats.vigilant.ui.dashboard.DashboardActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_alert_outline_red) // Use existing icon
            .setContentTitle("Security Risk Detected")
            .setContentText("Vigilant detected $appCount high-risk apps on your device.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(2001, notification)
    }
}
