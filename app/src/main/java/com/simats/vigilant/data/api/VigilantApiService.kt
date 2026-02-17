package com.simats.vigilant.data.api

import com.simats.vigilant.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface VigilantApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: Map<String, String>): Response<StandardResponse>

    @POST("auth/resend-otp")
    suspend fun resendOtp(): Response<StandardResponse>

    @POST("auth/revoke-all")
    suspend fun revokeAllSessions(): Response<StandardResponse>

    @POST("auth/change-password")
    @JvmSuppressWildcards
    suspend fun changePassword(@Body request: Map<String, String>): Response<StandardResponse>

    @POST("auth/forgot-password")
    @JvmSuppressWildcards
    suspend fun forgotPassword(@Body request: Map<String, String>): Response<StandardResponse>

    @POST("auth/reset-password")
    @JvmSuppressWildcards
    suspend fun resetPassword(@Body request: Map<String, String>): Response<StandardResponse>

    @POST("devices/register")
    suspend fun registerDevice(@Body deviceData: Map<String, String>): Response<StandardResponse>
    
    @GET("devices")
    suspend fun getDevices(@retrofit2.http.Query("current_device_id") currentDeviceId: String): Response<DeviceResponse>
    
    @POST("devices/{id}/revoke") // Using POST for safety/flexibility
    suspend fun revokeDevice(@retrofit2.http.Path("id") id: String): Response<StandardResponse> // or Int ID? DeviceItem has Int ID. 

    // --- Scans & Dashboard ---
    @GET("scan/dashboard")
    suspend fun getDashboardData(@retrofit2.http.Query("device_id") deviceId: String): Response<DashboardResponse>

    @GET("scan/status")
    suspend fun getScanStatus(@retrofit2.http.Query("scan_id") scanId: String): Response<ScanStatusResponse>
    
    @GET("scan/active")
    suspend fun getActiveScan(@retrofit2.http.Query("device_id") deviceId: String): Response<ScanStatusResponse>
    
    @POST("scan/{scan_id}/progress")
    @JvmSuppressWildcards
    suspend fun updateScanProgress(
        @retrofit2.http.Path("scan_id") scanId: String, 
        @Body progress: Map<String, Any>
    ): Response<StandardResponse>

    // --- Alerts ---
    @GET("alerts/recent")
    suspend fun getRecentAlerts(@retrofit2.http.Query("device_id") deviceId: String): Response<AlertsResponse>
    
    @GET("alerts/{id}")
    suspend fun getAlertDetails(@retrofit2.http.Path("id") alertId: String): Response<AlertDetailResponse>
    
    @POST("alerts/ack")
    @JvmSuppressWildcards
    suspend fun acknowledgeAlert(@Body request: Map<String, String>): Response<StandardResponse>
    
    // --- Settings ---
    @GET("settings/scan")
    suspend fun getScanSettings(): Response<SettingsResponse>
    
    @PUT("settings/scan")
    @JvmSuppressWildcards
    suspend fun updateScanSettings(@Body settings: Map<String, Any>): Response<StandardResponse>
    
    @GET("settings/alerts")
    suspend fun getAlertRules(): Response<SettingsResponse>
    
    @GET("settings/privacy")
    suspend fun getPrivacySettings(): Response<SettingsResponse>

    // --- Reports ---
    @GET("reports/weekly")
    suspend fun getWeeklyReports(): Response<WeeklyReportResponse>

    // --- Profile ---
    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse> 
    
    @PUT("profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<StandardResponse>

    @POST("profile/feedback")
    @JvmSuppressWildcards
    suspend fun submitFeedback(@Body request: Map<String, String>): Response<StandardResponse>

    // --- Legacy Scan (Keep if needed) ---
    @GET("scan/latest")
    suspend fun getLatestScan(@retrofit2.http.Query("device_id") deviceId: String): Response<ScanLatestResponse>
    
    @POST("scan/start")
    @JvmSuppressWildcards
    suspend fun startScan(@Body request: Map<String, String>): Response<ScanStartResponse> // Updated return type
    
    @POST("scan/complete")
    @JvmSuppressWildcards
    suspend fun completeScan(@Body request: Map<String, Any>): Response<StandardResponse>
    
    // Community
    @GET("community/threats")
    suspend fun getCommunityThreats(): Response<CommunityThreatResponse>

    @POST("community/reports")
    suspend fun submitReport(@Body report: Map<String, String>): Response<StandardResponse>
    
    @GET("community/my-reports")
    suspend fun getMyReports(): Response<MyReportsResponse>
    
    // Apps
    @GET("apps")
    suspend fun getInstalledApps(@retrofit2.http.Query("device_id") deviceId: String): Response<AppListResponse>
    
    @GET("apps/{package_name}")
    suspend fun getAppDetails(@retrofit2.http.Path("package_name") packageName: String, @retrofit2.http.Query("device_id") deviceId: String): Response<AppDetailResponse>
    
    // Chat
    @POST("chat/message")
    @JvmSuppressWildcards
    suspend fun chatMessage(@Body request: Map<String, Any>): Response<ChatResponse>
}
