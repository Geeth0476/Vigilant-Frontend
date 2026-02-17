package com.simats.vigilant

data class CommunityThreat(
    val id: String,
    val appName: String,
    val packageName: String,
    val category: String, // Spyware, Stalkerware, etc.
    val riskLevel: String, // Critical, High, Medium, Low
    val reportCount: Int,
    val firstSeen: String,
    val lastReported: String,
    val behaviors: List<String>,
    val description: String
)

object CommunityRepository {
    var threats = mutableListOf(
        CommunityThreat(
            "1", "Call Recorder Plus", "com.call.rec.plus", "Spyware", "Critical", 
            127, "Jan 2026", "Just now",
            listOf("Background recording", "Hidden icon"), 
            "Accessing microphone without user action and hiding from launcher."
        ),
        CommunityThreat(
            "2", "Find My Kids Tracker", "com.find.kids", "Stalkerware", "High", 
            84, "Dec 2025", "2 hours ago",
            listOf("Location sharing", "SMS reading"), 
            "Sending location data to unknown server."
        ),
        CommunityThreat(
            "3", "Super Flashlight", "com.super.light", "Permission Abuse", "Medium", 
            42, "Nov 2025", "Yesterday",
            listOf("Contacts access", "Camera access"), 
            "Requests contacts permission unnecessarily."
        ),
        CommunityThreat(
            "4", "Cleaner Pro", "com.clean.pro", "Adware", "Low", 
            15, "Jan 2026", "Today",
            listOf("Full screen ads"), 
            "Displaying intrusive ads on lock screen."
        ),
        CommunityThreat(
            "5", "Weather Daily", "com.weather.daily", "Data Theft", "High", 
            56, "Oct 2025", "5 hours ago",
            listOf("Clipboard access", "Background data"), 
            "Uploading clipboard data to remote server."
        )
    )
    
    fun updateThreats(newThreats: List<CommunityThreat>) {
        threats.clear()
        threats.addAll(newThreats)
    }
    
    fun getThreatById(id: String): CommunityThreat? {
        return threats.find { it.id == id }
    }
    
    fun getThreatByName(name: String): CommunityThreat? {
        return threats.find { it.appName.equals(name, ignoreCase = true) }
    }
}
