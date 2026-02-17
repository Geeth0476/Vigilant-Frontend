package com.simats.vigilant.data

import android.content.Context
import android.content.SharedPreferences
import com.simats.vigilant.SecurityAlert
import java.text.SimpleDateFormat
import java.util.*

class ScanLocalDataSource(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "vigilant_scan_data"
        private const val KEY_LAST_SCAN_TIME = "last_scan_time"
        private const val KEY_RISK_SCORE = "risk_score"
        private const val KEY_RISK_LEVEL = "risk_level"
        private const val KEY_TOP_RISK_APP = "top_risk_app"
        private const val KEY_ALERTS = "alerts_list"
    }
    
    // In-memory cache for dynamic session data
    private var cachedAlerts: List<SecurityAlert> = emptyList()
    
    enum class RiskLevel {
        SAFE, MEDIUM, HIGH
    }
    
    private fun getPrefs(): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Save scan results
    fun saveScanResult(riskScore: Int, riskLevel: RiskLevel) {
        getPrefs().edit().apply {
            putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis())
            putInt(KEY_RISK_SCORE, riskScore)
            putString(KEY_RISK_LEVEL, riskLevel.name)
            apply()
        }
    }
    
    fun saveTopRiskApp(packageName: String?) {
        getPrefs().edit().putString(KEY_TOP_RISK_APP, packageName).apply()
    }
    
    fun getTopRiskApp(): String? {
        return getPrefs().getString(KEY_TOP_RISK_APP, null)
    }
    
    // Get risk score
    fun getRiskScore(): Int {
        return getPrefs().getInt(KEY_RISK_SCORE, 0)
    }
    
    // Get risk level
    fun getRiskLevel(): RiskLevel {
        val levelName = getPrefs().getString(KEY_RISK_LEVEL, RiskLevel.SAFE.name)
        return try {
            RiskLevel.valueOf(levelName!!)
        } catch (e: Exception) {
            RiskLevel.SAFE
        }
    }
    
    // Get last scan time formatted
    fun getLastScanTime(): String {
        val lastScanMillis = getPrefs().getLong(KEY_LAST_SCAN_TIME, 0)
        
        if (lastScanMillis == 0L) {
            return "Never scanned"
        }
        
        val now = System.currentTimeMillis()
        val diff = now - lastScanMillis
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(lastScanMillis))
            }
        }
    }
    
    // Save alerts to prefs
    fun saveAlerts(alerts: List<SecurityAlert>) {
        cachedAlerts = alerts
        try {
            val gson = com.google.gson.Gson()
            val json = gson.toJson(alerts)
            getPrefs().edit().putString(KEY_ALERTS, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getRecentAlerts(): List<SecurityAlert> {
        if (cachedAlerts.isNotEmpty()) {
            return cachedAlerts
        }
        
        // Try to load from prefs
        val json = getPrefs().getString(KEY_ALERTS, null)
        if (json != null) {
            try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<SecurityAlert>>() {}.type
                cachedAlerts = gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return cachedAlerts
    }
}
