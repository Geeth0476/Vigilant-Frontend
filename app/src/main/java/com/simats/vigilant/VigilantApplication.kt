package com.simats.vigilant

import android.app.Application
import com.simats.vigilant.data.ScanLocalDataSource
import com.simats.vigilant.data.repository.ScanRepository

class VigilantApplication : Application(), Application.ActivityLifecycleCallbacks {

    lateinit var scanRepository: ScanRepository
    
    // Lifecycle tracking
    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onCreate() {
        super.onCreate()
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        val dataSource = ScanLocalDataSource(this)
        scanRepository = ScanRepository(dataSource, this)
        
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}

    override fun onActivityStarted(activity: android.app.Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // App enters foreground
            checkAppLock(activity)
        }
    }

    override fun onActivityResumed(activity: android.app.Activity) {}

    override fun onActivityPaused(activity: android.app.Activity) {}

    override fun onActivityStopped(activity: android.app.Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // App enters background
        }
    }

    override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}

    override fun onActivityDestroyed(activity: android.app.Activity) {}
    
    private fun checkAppLock(activity: android.app.Activity) {
        // Exclude screens that shouldn't trigger lock
        if (activity is LockScreenActivity || 
            activity is SplashActivity || 
            activity is LoginActivity || 
            activity is CreateAccountActivity ||
            activity is WelcomeActivity) {
             return
        }
        
        val prefs = getSharedPreferences("vigilant_prefs", android.content.Context.MODE_PRIVATE)
        val pin = prefs.getString("app_pin", "")
        
        if (!pin.isNullOrEmpty()) {
             val intent = android.content.Intent(activity, LockScreenActivity::class.java)
             intent.flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
             activity.startActivity(intent)
        }
    }
}
