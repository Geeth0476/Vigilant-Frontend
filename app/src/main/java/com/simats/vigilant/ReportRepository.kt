package com.simats.vigilant

object ReportRepository {
    data class ReportItem(val name: String, val type: String, val date: String, val status: String)
    
    val reports = mutableListOf<ReportItem>(
        ReportItem("Unknown App", "Spyware", "Today", "Under Review"),
        ReportItem("Super Flashlight", "Permission Abuse", "Yesterday", "Resolved"),
        ReportItem("Calculator+", "Suspicious Behavior", "2 days ago", "Under Review")
    )
    
    fun addReport(report: ReportItem) {
        reports.add(0, report)
    }
    
    fun clear() {
        reports.clear()
    }
}
