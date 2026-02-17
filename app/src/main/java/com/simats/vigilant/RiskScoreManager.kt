package com.simats.vigilant

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object RiskScoreManager {

    data class RiskResult(
        val totalScore: Int,
        val riskLevel: RiskLevel,
        val riskFactors: List<RiskFactor>
    )

    enum class RiskLevel(val label: String, val colorResId: Int) {
        SAFE("Safe", R.color.vigilant_green),
        LOW("Low Risk", R.color.vigilant_green),
        MEDIUM("Medium Risk", R.color.vigilant_amber),
        HIGH("High Risk", R.color.vigilant_red),
        CRITICAL("Critical", R.color.vigilant_red)
    }

    data class RiskFactor(
        val description: String,
        val score: Int,
        val type: FactorType
    )

    enum class FactorType {
        PERMISSION,
        BEHAVIOR,
        RUNTIME,
        MODIFIER
    }

    // --- Scoring Constants ---

    // 1. Permission Weights
    private val PERM_WEIGHTS = mapOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to 25,
        "android.permission.READ_SMS" to 15,
        "android.permission.RECEIVE_SMS" to 15,
        "android.permission.SEND_SMS" to 15,
        "android.permission.READ_CALL_LOG" to 15,
        "android.permission.PROCESS_OUTGOING_CALLS" to 15,
        "android.permission.RECORD_AUDIO" to 15,
        "android.permission.CAMERA" to 10,
        "android.permission.ACCESS_FINE_LOCATION" to 12, // Precise location is valuable data
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 15,
        "android.permission.ACCESS_COARSE_LOCATION" to 5,
        "android.permission.READ_CONTACTS" to 10,
        "android.permission.GET_ACCOUNTS" to 8,
        "android.permission.READ_PHONE_STATE" to 8, // Device ID tracking
        "android.permission.SYSTEM_ALERT_WINDOW" to 12, // Overlay attacks
        "android.permission.PACKAGE_USAGE_STATS" to 15, // Usage tracking
        "android.permission.REQUEST_INSTALL_PACKAGES" to 12, // Sideloading authority
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to 18 // Intercept notifications (2FA)
    )

    // 2. Behavior/Combination Weights
    private const val SCORE_BOOT = 5
    private const val SCORE_FOREGROUND_SERVICE = 5 
    
    // 3. Modifiers
    private const val MODIFIER_NEW_INSTALL = 5 // < 3 days
    private const val MODIFIER_OLD_INSTALL = -5 // > 6 months
    
    private val SUSPICIOUS_KEYWORDS = listOf("tracker", "spy", "monitoring", "stealth", "agent", "rat", "keylogger", "cloner")

    fun calculateRiskScore(packageInfo: PackageInfo, pm: PackageManager): RiskResult {
        val factors = mutableListOf<RiskFactor>()
        var permScore = 0
        var behaviorScore = 0
        var modifierScore = 0

        // --- 1. Permissions ---
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        val permList = requestedPermissions.toList()

        PERM_WEIGHTS.forEach { (perm, score) ->
            if (permList.contains(perm)) {
                permScore += score
                val niceName = perm.substringAfterLast(".").replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                factors.add(RiskFactor("Requests $niceName", score, FactorType.PERMISSION))
            }
        }
        
        // Cap Permission Score at 65 to ensure behaviors are needed for Critical
        if (permScore > 65) permScore = 65

        // --- 2. Behaviors (Static Inference) ---
        
        // Starts on Boot
        if (permList.contains("android.permission.RECEIVE_BOOT_COMPLETED")) {
            behaviorScore += SCORE_BOOT
            factors.add(RiskFactor("Runs automatically at startup", SCORE_BOOT, FactorType.BEHAVIOR))
        }

        // Combinations (Inferred)
        // Mic + Accessibility = Very dangerous
        if (permList.contains("android.permission.RECORD_AUDIO") && permList.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")) {
            behaviorScore += 20
            factors.add(RiskFactor("High Risk: Can record audio and control screen", 20, FactorType.BEHAVIOR))
        }
        
        // Location + Background
        if (permList.contains("android.permission.ACCESS_FINE_LOCATION") && permList.contains("android.permission.ACCESS_BACKGROUND_LOCATION")) {
            behaviorScore += 10
            factors.add(RiskFactor("Tracks location in background", 10, FactorType.BEHAVIOR))
        }
        
        // SMS + Contacts
        if (permList.contains("android.permission.READ_SMS") && permList.contains("android.permission.READ_CONTACTS")) {
             behaviorScore += 10
             factors.add(RiskFactor("Accesses both SMS and Contacts", 10, FactorType.BEHAVIOR))
        }

        // Cap Behavior Score at 40
        if (behaviorScore > 40) behaviorScore = 40

        // --- 3. Modifiers ---
        val installTime = packageInfo.firstInstallTime
        val now = System.currentTimeMillis()
        val daysInstalled = (now - installTime) / (1000 * 60 * 60 * 24)

        if (daysInstalled < 3) {
            modifierScore += MODIFIER_NEW_INSTALL
            factors.add(RiskFactor("Recently installed (< 3 days)", MODIFIER_NEW_INSTALL, FactorType.MODIFIER))
        } else if (daysInstalled > 180) {
            modifierScore += MODIFIER_OLD_INSTALL
            factors.add(RiskFactor("Trusted (Installed > 6 months)", MODIFIER_OLD_INSTALL, FactorType.MODIFIER))
        }
        
        // Keyword Check
        val pkgNameLower = packageInfo.packageName.lowercase()
        // Add "mod" and variants to keywords
        val EXTENDED_KEYWORDS = SUSPICIOUS_KEYWORDS + listOf("mod", "crack", "unlocked", "patched", "hack")
        
        EXTENDED_KEYWORDS.forEach { keyword ->
            if (pkgNameLower.contains(keyword)) {
                modifierScore += 20
                factors.add(RiskFactor("Suspicious package name: '$keyword'", 20, FactorType.MODIFIER))
            }
        }
        
        // --- 4. Installation Source Check (Anti-Mod / Sideload Detection) ---
        try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageInfo.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageInfo.packageName)
            }
            
            // Known safe installers
            val safeInstallers = listOf("com.android.vending", "com.google.android.feedback", "com.amazon.venezia")
            
            if (installer == null) {
                // Sideloaded (Manual Install via APK)
                modifierScore += 25
                factors.add(RiskFactor("Sideloaded (Installed manually via APK)", 25, FactorType.MODIFIER))
            } else if (!safeInstallers.contains(installer)) {
                // Installed by third-party store or unknown agent
                modifierScore += 15
                factors.add(RiskFactor("Installed from third-party source ($installer)", 15, FactorType.MODIFIER))
            } else {
                 // Play Store bonus? Maybe not bonus, but no penalty.
                 // factors.add(RiskFactor("Verified Source (Play Store)", -5, FactorType.MODIFIER))
            }
        } catch (e: Exception) {
            // Ignore checks if we can't determine source
        }

        // Calculate Final
        var finalScore = permScore + behaviorScore + modifierScore
        if (finalScore < 0) finalScore = 0
        if (finalScore > 100) finalScore = 100
        
        // Determine Level
        val level = when {
            finalScore <= 20 -> RiskLevel.SAFE
            finalScore <= 40 -> RiskLevel.LOW
            finalScore <= 70 -> RiskLevel.MEDIUM
            finalScore <= 90 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }

        return RiskResult(finalScore, level, factors)
    }
}
