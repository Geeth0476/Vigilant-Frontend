package com.simats.vigilant.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.simats.vigilant.VigilantApplication
import com.simats.vigilant.SecurityAlert
import com.simats.vigilant.data.ScanLocalDataSource
import com.simats.vigilant.data.repository.ScanRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = getApplication<VigilantApplication>().scanRepository

    private val _riskScore = MutableLiveData<Int>()
    val riskScore: LiveData<Int> = _riskScore

    private val _scanStatus = MutableLiveData<String>()
    val scanStatus: LiveData<String> = _scanStatus
    
    // Risk Level derived from score or repo
    private val _riskLevel = MutableLiveData<ScanLocalDataSource.RiskLevel>()
    val riskLevel: LiveData<ScanLocalDataSource.RiskLevel> = _riskLevel

    private val _alerts = MutableLiveData<List<SecurityAlert>>()
    val alerts: LiveData<List<SecurityAlert>> = _alerts

    private val _topRiskApp = MutableLiveData<String?>()
    val topRiskApp: LiveData<String?> = _topRiskApp

    // Polling Job
    private var pollingJob: kotlinx.coroutines.Job? = null

    // Status variables
    private val _isProtected = MutableLiveData<Boolean>()
    val isProtected: LiveData<Boolean> = _isProtected
    
    // Active Scan Status
    private val _activeScanProgress = MutableLiveData<Int?>()
    val activeScanProgress: LiveData<Int?> = _activeScanProgress

    init {
        // Load initial state from local repository immediately
        refreshDataFromLocal()
    }
    
    fun refreshDataFromLocal() {
        try {
            _riskScore.value = repository.getRiskScore()
            _riskLevel.value = repository.getRiskLevel()
            _scanStatus.value = repository.getLastScanTime()
            _isProtected.value = repository.getRiskLevel() == ScanLocalDataSource.RiskLevel.SAFE
            
            val localAlerts = repository.getRecentAlerts()
            _alerts.value = localAlerts
            
            val topPkg = repository.getTopRiskApp()
            _topRiskApp.value = topPkg
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDashboardPolling() {
        if (pollingJob?.isActive == true) return
        
        pollingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val context = getApplication<VigilantApplication>()
            val api = com.simats.vigilant.data.api.ApiClient.getService(context)
            
            while (isActive) {
                try {
                    val deviceId = com.simats.vigilant.data.DeviceIdManager.getDeviceId(context)
                    
                    // 1. Fetch Aggregated Dashboard Data
                    val response = api.getDashboardData(deviceId)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            
                            // A. Update Risk Score
                            if (data.risk_score != null) {
                                _riskScore.postValue(data.risk_score.score)
                                
                                val level = if (data.risk_score.level == "HIGH") ScanLocalDataSource.RiskLevel.HIGH 
                                         else if(data.risk_score.level == "MEDIUM") ScanLocalDataSource.RiskLevel.MEDIUM 
                                         else ScanLocalDataSource.RiskLevel.SAFE
                                
                                _riskLevel.postValue(level)
                                _isProtected.postValue(level == ScanLocalDataSource.RiskLevel.SAFE)
                                
                                // Save to local repo for persistence
                                repository.saveScanResult(data.risk_score.score, level)
                            }
                            
                            // B. Update Scan Status/Time
                            val lastTime = data.risk_score?.last_scan_time ?: "Never"
                            _scanStatus.postValue(lastTime)
                            
                            // C. Active Scan Monitoring
                            if (data.active_scan != null && data.active_scan.status == "RUNNING") {
                                _activeScanProgress.postValue(data.active_scan.progress_percent)
                            } else {
                                _activeScanProgress.postValue(null)
                            }
                            
                            // D. Top Risky Apps
                            if (!data.top_risky_apps.isNullOrEmpty()) {
                                _topRiskApp.postValue(data.top_risky_apps[0].package_name)
                            } else {
                                _topRiskApp.postValue(null)
                            }
                            
                            // E. Alerts
                            // If we have alerts reported, fetch the details to show them
                            if (data.recent_alerts_count > 0) {
                                val alerts = repository.fetchBackendAlerts()
                                _alerts.postValue(alerts)
                            } else {
                                _alerts.postValue(emptyList())
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Poll every 5 seconds
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
    }
    
    // Legacy refresh (can call once or trigger polling)
    fun refreshDashboardState() {
        startDashboardPolling()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
