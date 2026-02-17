package com.simats.vigilant.data.model

// --- Scan & Dashboard ---

data class DashboardResponse(
    val success: Boolean,
    val data: DashboardData?
)

data class DashboardData(
    val risk_score: RiskScoreData?,
    val active_scan: ScanStatusData?,
    val recent_alerts_count: Int,
    val top_risky_apps: List<InstalledAppModel>?,
    val scan_history: ScanHistoryData?
)

data class RiskScoreData(
    val score: Int,
    val level: String,
    val updated_at: String?,
    val last_scan_time: String?
)

data class ScanStatusResponse(
    val success: Boolean,
    val data: ScanStatusData?
)

data class ScanStatusData(
    val scan_id: String,
    val status: String,
    val progress_percent: Int,
    val apps_scanned: Int,
    val app_count: Int,
    val mode: String,
    val started_at: String?
)

data class ScanHistoryData(
    val total_scans: Int,
    val high_risk_scans: Int,
    val avg_risk_score: Double
)

// --- Scan Legacy (Keep for compatibility if needed) ---
data class ScanLatestResponse(
    val success: Boolean,
    val data: ScanResultData?
)

data class ScanResultData(
    val overall_risk_score: Int,
    val overall_risk_level: String,
    val completed_at: String
)

// --- Apps ---
data class AppListResponse(
    val success: Boolean,
    val data: List<InstalledAppModel>?
)

data class AppDetailResponse(
    val success: Boolean,
    val data: InstalledAppModel?
)

data class InstalledAppModel(
    val app_name: String,
    val package_name: String,
    val version_name: String?,
    val is_system_app: Int,
    val risk_score: Int?,
    val risk_level: String?,
    val top_factor_desc: String?
)

// --- Community ---
data class CommunityThreatResponse(
    val success: Boolean,
    val data: List<CommunityThreat>?
)

data class CommunityThreat(
    val id: Int,
    val app_name: String,
    val package_name: String,
    val category: String,
    val risk_level: String,
    val report_count: Int,
    val description: String,
    val last_reported_at: String?,
    val first_seen_at: String?,
    val behaviors: String? // JSON or CSV
)

// --- Alerts ---
data class AlertsResponse(
    val success: Boolean,
    val data: List<SecurityAlert>?
)

data class AlertDetailResponse(
    val success: Boolean,
    val data: SecurityAlert?
)

data class SecurityAlert(
    val id: String, // Public ID
    val type: String,
    val severity: String,
    val title: String,
    val description: String,
    val is_acknowledged: Int,
    val created_at: String,
    val recommendations: List<String>?
)

// --- Settings ---
data class SettingsResponse(
    val success: Boolean,
    val data: Map<String, Any>? // Generic map for now as settings vary
)

// --- Reports ---
data class WeeklyReportResponse(
    val success: Boolean,
    val data: List<WeeklyReportSummary>?
)

data class WeeklyReportSummary(
    val id: Int,
    val week_start_date: String,
    val week_end_date: String,
    val risk_score_avg: Int,
    val threats_blocked: Int
)

// --- Profile ---
data class RegisterRequest(
    val email: String,
    val password: String,
    val full_name: String,
    val phone: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String,
    val device_id: String? = null,
    val device_model: String? = null,
    val os_version: String? = null
)

data class StandardResponse(
    val success: Boolean,
    val message: String?,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String
)

data class LoginResponse(
    val success: Boolean,
    val data: LoginData?,
    val error: ApiError?
)

data class LoginData(
    val user_id: Int,
    val access_token: String,
    val full_name: String,
    val is_premium: Boolean = false,
    val is_verified: Boolean = true,
    val phone: String?, 
    val profile_image: String?,
    val message: String?
)

data class ProfileResponse(
    val success: Boolean,
    val data: ProfileData?
)

data class ProfileData(
    val full_name: String?,
    val email: String?,
    val created_at: String?,
    val phone: String?,
    val profile_image: String?,
    val device_count: Int?
)

data class UpdateProfileRequest(
    val full_name: String?,
    val email: String?,
    val phone: String?,
    val profile_image: String?,
    val current_password: String?,
    val new_password: String?
)

data class MyReportsResponse(
    val success: Boolean,
    val data: List<MyReportItem>?
)

data class MyReportItem(
    val id: Int,
    val app_name: String,
    val package_name: String?,
    val report_type: String, // e.g., "False Positive"
    val description: String,
    val status: String, // "Pending", "Resolved"
    val created_at: String
)

data class DeviceResponse(
    val success: Boolean,
    val data: List<DeviceItem>?
)

data class DeviceItem(
    val id: Int,
    val device_name: String,
    val model: String?,
    val manufacturer: String?,
    val last_active: String?,
    val is_current: Boolean?
)

// --- Chat ---
data class ChatResponse(
    val success: Boolean,
    val data: ChatData?,
    val error: ApiError?
)

data class ChatData(
    val response: String,
    val suggestions: List<String>?
)
