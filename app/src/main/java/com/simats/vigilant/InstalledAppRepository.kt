package com.simats.vigilant

import android.graphics.drawable.Drawable

object InstalledAppRepository {
    data class InstalledApp(
        val packageName: String,
        val appName: String,
        val riskScore: Int,
        val riskDescription: String,
        val riskFactors: List<RiskScoreManager.RiskFactor> = emptyList()
    )
    
    val installedApps = mutableListOf<InstalledApp>()
    
    fun clear() {
        installedApps.clear()
    }
    
    fun addApp(app: InstalledApp) {
        installedApps.add(app)
    }

    fun populate(context: android.content.Context) {
        // Always refresh


        val pm = context.packageManager
        val installed = pm.getInstalledPackages(android.content.pm.PackageManager.GET_META_DATA or android.content.pm.PackageManager.GET_PERMISSIONS)
        
        val userApps = installed.filter { 
             val appInfo = it.applicationInfo
             if (appInfo != null) {
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0) ||
                (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)
             } else {
                 false
             }
        }
        
        clear()
        
        userApps.forEach { packageInfo ->
             val appInfo = packageInfo.applicationInfo
             val riskResult = RiskScoreManager.calculateRiskScore(packageInfo, pm)
             val topFactor = riskResult.riskFactors.maxByOrNull { it.score }
             val riskDesc = topFactor?.description ?: "No suspicious behavior detected"

             val app = InstalledApp(
                packageInfo.packageName,
                appInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName,
                riskResult.totalScore,
                riskDesc,
                riskResult.riskFactors
             )
             addApp(app)
        }
    }
}
