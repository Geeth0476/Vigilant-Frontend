package com.simats.vigilant

import android.os.Parcelable
data class SecurityAlert(
    val id: String,
    val title: String,
    val description: String,
    val detailedInfo: String,
    val severity: Severity,
    val timestamp: Long,
    val recommendations: List<String>,
    val packageName: String? = null
) : Parcelable {
    
    enum class Severity {
        HIGH, MEDIUM, LOW
    }

    constructor(parcel: android.os.Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        Severity.valueOf(parcel.readString() ?: "LOW"),
        parcel.readLong(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(detailedInfo)
        parcel.writeString(severity.name)
        parcel.writeLong(timestamp)
        parcel.writeStringList(recommendations)
        parcel.writeString(packageName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<SecurityAlert> {
             override fun createFromParcel(parcel: android.os.Parcel): SecurityAlert {
                return SecurityAlert(parcel)
            }

            override fun newArray(size: Int): Array<SecurityAlert?> {
                return arrayOfNulls(size)
            }
        }

        // Predefined alert templates
        private val alertTemplates = listOf(
            AlertTemplate(
                "Spyware Behavior",
                "Recorder accessed microphone while screen was off",
                "The app 'Voice Recorder' accessed your microphone when the screen was turned off. This is a common spyware behavior pattern that could indicate unauthorized audio recording.\n\nDetails:\n• App: Voice Recorder\n• Permission: RECORD_AUDIO\n• Time: Screen off mode\n• Duration: 2 minutes 34 seconds\n• Frequency: 3 times in last 24 hours",
                Severity.HIGH,
                listOf(
                    "Revoke microphone permission for this app",
                    "Uninstall the app if not needed",
                    "Check app reviews for similar reports",
                    "Monitor battery usage for unusual drain"
                )
            ),
            AlertTemplate(
                "Suspicious Permission",
                "Flashlight requested location access",
                "The app 'Flashlight' requested access to your precise location. This permission is unnecessary for a flashlight app and may indicate data collection.\n\nDetails:\n• App: Flashlight\n• Permission: ACCESS_FINE_LOCATION\n• Justification: None provided\n• Background access: Requested\n• Data sharing: Unknown",
                Severity.MEDIUM,
                listOf(
                    "Deny location permission",
                    "Consider using built-in flashlight",
                    "Report app to Play Store",
                    "Check app's privacy policy"
                )
            ),
            AlertTemplate(
                "Background Activity",
                "Photo Editor sending data at night",
                "The app 'Photo Editor' transmitted 45MB of data between 2 AM and 4 AM while you were likely asleep. This unusual background activity warrants investigation.\n\nDetails:\n• App: Photo Editor\n• Data sent: 45.2 MB\n• Time: 2:15 AM - 4:23 AM\n• Destination: Unknown servers\n• Network: WiFi",
                Severity.HIGH,
                listOf(
                    "Restrict background data for this app",
                    "Check app permissions",
                    "Review recent photos for unauthorized uploads",
                    "Consider alternative photo editing apps"
                )
            ),
            AlertTemplate(
                "Camera Access",
                "Weather app accessed camera",
                "The app 'Weather Forecast' accessed your camera without clear justification. Weather apps typically don't require camera access.\n\nDetails:\n• App: Weather Forecast\n• Permission: CAMERA\n• Usage: 1 time\n• Duration: 12 seconds\n• Photos taken: Unknown",
                Severity.MEDIUM,
                listOf(
                    "Revoke camera permission",
                    "Check for unauthorized photos",
                    "Use trusted weather apps only",
                    "Enable camera access notifications"
                )
            ),
            AlertTemplate(
                "SMS Access",
                "Game reading text messages",
                "The app 'Puzzle Game' requested permission to read your SMS messages. Games should not need access to your private messages.\n\nDetails:\n• App: Puzzle Game\n• Permission: READ_SMS\n• Justification: 'Verify phone number'\n• Messages accessed: Unknown\n• Risk: Identity theft, OTP stealing",
                Severity.HIGH,
                listOf(
                    "Deny SMS permission immediately",
                    "Uninstall the game",
                    "Change important passwords",
                    "Monitor bank accounts for suspicious activity"
                )
            ),
            AlertTemplate(
                "Contact Access",
                "Calculator exported contacts",
                "The app 'Calculator Pro' accessed and potentially exported your contact list. Calculator apps don't need contact access.\n\nDetails:\n• App: Calculator Pro\n• Permission: READ_CONTACTS\n• Contacts accessed: All (247 contacts)\n• Data sent: 1.2 MB\n• Destination: Third-party server",
                Severity.HIGH,
                listOf(
                    "Revoke contacts permission",
                    "Uninstall immediately",
                    "Warn contacts about potential spam",
                    "Use built-in calculator app"
                )
            ),
            AlertTemplate(
                "Excessive Permissions",
                "Wallpaper app requests 12 permissions",
                "The app 'HD Wallpapers' is requesting 12 different permissions, far more than necessary for displaying wallpapers.\n\nDetails:\n• App: HD Wallpapers\n• Permissions requested: 12\n• Necessary: 2 (Storage, Internet)\n• Unnecessary: 10 including Camera, Microphone, Location\n• Privacy risk: High",
                Severity.MEDIUM,
                listOf(
                    "Deny unnecessary permissions",
                    "Use alternative wallpaper apps",
                    "Review all granted permissions",
                    "Check app developer reputation"
                )
            )
        )
        
        fun createAlertFromApp(appPackage: String, appName: String, riskScore: Int, riskFactors: List<String>): SecurityAlert {
            val severity = when {
                riskScore >= 80 -> Severity.HIGH
                riskScore >= 50 -> Severity.MEDIUM
                else -> Severity.LOW
            }
            
            val factorDesc = riskFactors.firstOrNull() ?: "Suspicious activity detected"
            
            return SecurityAlert(
                id = "alert_${System.currentTimeMillis()}_${appPackage.hashCode()}",
                title = "Risk Detected: $appName",
                description = factorDesc,
                detailedInfo = "The app '$appName' ($appPackage) has been flagged with a risk score of $riskScore.\n\nFactors:\n• " + riskFactors.joinToString("\n• "),
                severity = severity,
                timestamp = System.currentTimeMillis(),
                recommendations = listOf(
                    "Review permissions for this app",
                    "Consider uninstalling if not used",
                    "Check data usage"
                ),
                packageName = appPackage
            )
        }

        fun generateRandomAlerts(count: Int = 2): List<SecurityAlert> {
            val shuffled = alertTemplates.shuffled()
            val selected = shuffled.take(count)
            val currentTime = System.currentTimeMillis()
            
            return selected.mapIndexed { index, template ->
                SecurityAlert(
                    id = "alert_${currentTime}_$index",
                    title = template.title,
                    description = template.description,
                    detailedInfo = template.detailedInfo,
                    severity = template.severity,
                    timestamp = currentTime - (index * 60000 * (2..30).random()), // Random time ago
                    recommendations = template.recommendations,
                    packageName = null // Templates don't link to real apps on the device
                )
            }
        }
    }
    
    private data class AlertTemplate(
        val title: String,
        val description: String,
        val detailedInfo: String,
        val severity: Severity,
        val recommendations: List<String>
    )
}
