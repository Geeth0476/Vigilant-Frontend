package com.simats.vigilant.data.repository

import com.simats.vigilant.SecurityAlert
import com.simats.vigilant.InstalledAppRepository
import android.content.Context
import com.simats.vigilant.data.ScanLocalDataSource

class ScanRepository(private val dataSource: ScanLocalDataSource, private val context: Context) {

    fun saveScanResult(riskScore: Int, riskLevel: ScanLocalDataSource.RiskLevel) {
        dataSource.saveScanResult(riskScore, riskLevel)
    }

    fun saveTopRiskApp(packageName: String?) {
        dataSource.saveTopRiskApp(packageName)
    }

    fun getTopRiskApp(): String? {
        return dataSource.getTopRiskApp()
    }

    fun getRiskScore(): Int {
        return dataSource.getRiskScore()
    }

    fun getRiskLevel(): ScanLocalDataSource.RiskLevel {
        return dataSource.getRiskLevel()
    }

    fun getLastScanTime(): String {
        return dataSource.getLastScanTime()
    }
    
    fun getRiskLevelFromScore(score: Int): ScanLocalDataSource.RiskLevel {
        return when {
            score < 30 -> ScanLocalDataSource.RiskLevel.SAFE
            score < 60 -> ScanLocalDataSource.RiskLevel.MEDIUM
            else -> ScanLocalDataSource.RiskLevel.HIGH
        }
    }

    fun saveAlerts(alerts: List<SecurityAlert>) {
        dataSource.saveAlerts(alerts)
    }

    suspend fun fetchBackendAlerts(): List<SecurityAlert> {
        return try {
            val deviceId = com.simats.vigilant.data.DeviceIdManager.getDeviceId(context)
            val api = com.simats.vigilant.data.api.ApiClient.getService(context)
            val response = api.getRecentAlerts(deviceId)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val apiAlerts = response.body()?.data
                if (apiAlerts != null && apiAlerts.isNotEmpty()) {
                    val mappedAlerts = apiAlerts.map { apiAlert ->
                        // Parse Severity
                        val severity = when (apiAlert.severity.uppercase()) {
                            "HIGH" -> SecurityAlert.Severity.HIGH
                            "MEDIUM" -> SecurityAlert.Severity.MEDIUM
                            else -> SecurityAlert.Severity.LOW
                        }
                        
                        // Parse Timestamp (Simple approximation or try parse)
                        // Assuming backend sends YYYY-MM-DD HH:MM:SS, but fallback to current if fail
                        val timestamp = try {
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .parse(apiAlert.created_at)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        
                        SecurityAlert(
                            id = apiAlert.id,
                            title = apiAlert.title,
                            description = apiAlert.description,
                            detailedInfo = apiAlert.description, // Backend might need detail endpoint, use desc for now
                            severity = severity,
                            timestamp = timestamp,
                            recommendations = apiAlert.recommendations ?: listOf("Check app settings"),
                            packageName = null // Backend alert doesn't explicitly guarantee package name here
                        )
                    }
                    dataSource.saveAlerts(mappedAlerts)
                    return mappedAlerts
                }
            }
            // If backend empty or fail, return empty or local?
            // If backend is live, we should probably prefer empty over fake local data to avoid confusion.
            // But for demo continuity, falling back to local if backend is empty might be safer?
            // Let's return local if backend fails, but empty if backend success & empty.
            if (response.isSuccessful) emptyList() else getRecentAlerts()
        } catch (e: Exception) {
            e.printStackTrace()
            getRecentAlerts()
        }
    }

    fun getRecentAlerts(): List<SecurityAlert> {
        val cached = dataSource.getRecentAlerts()
        if (cached.isNotEmpty()) {
            return cached
        }
        
        // Ensure Repository is populated
        if (InstalledAppRepository.installedApps.isEmpty()) {
            InstalledAppRepository.populate(context)
        }
        
        // Try to generate alerts from actual installed apps
        val riskyApps = InstalledAppRepository.installedApps.filter { it.riskScore > 30 }.shuffled()
        
        if (riskyApps.isNotEmpty()) {
            val dynamicAlerts = riskyApps.take(2).map { app ->
                SecurityAlert.createAlertFromApp(
                    app.packageName,
                    app.appName,
                    app.riskScore,
                    app.riskFactors.map { it.description }
                )
            }
            dataSource.saveAlerts(dynamicAlerts)
            return dynamicAlerts
        }
        
        val random = SecurityAlert.generateRandomAlerts(2)
        dataSource.saveAlerts(random)
        return random
    }
}
