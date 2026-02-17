package com.simats.vigilant

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.vigilant.ui.dashboard.DashboardActivity

abstract class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation(bottomNavigationView: BottomNavigationView, selectedItemId: Int) {
        bottomNavigationView.selectedItemId = selectedItemId
        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) return@setOnItemSelectedListener true
            
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    navigateTo(DashboardActivity::class.java)
                    true
                }
                R.id.nav_apps -> {
                    navigateTo(InstalledAppsActivity::class.java)
                    true
                }
                R.id.nav_community -> {
                    navigateTo(CommunityFeedActivity::class.java)
                    true
                }
                R.id.nav_profile -> {
                    navigateTo(ProfileAccountActivity::class.java)
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.setOnItemReselectedListener {} 
    }

    private fun navigateTo(clazz: Class<*>) {
        if (this::class.java == clazz) return

        val navOrder = mapOf(
            DashboardActivity::class.java to 0,
            InstalledAppsActivity::class.java to 1,
            CommunityFeedActivity::class.java to 2,
            ProfileAccountActivity::class.java to 3
        )

        val currentIndex = navOrder[this::class.java] ?: 0
        val targetIndex = navOrder[clazz] ?: 0

        val intent = Intent(this, clazz)
        
        if (clazz == DashboardActivity::class.java) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        } else {
             startActivity(intent)
             // Use finish() only if strictly effectively replacing the root of the "stack" visually
             // But actually BottomNav usually keeps activities or reorders. 
             // Current app logic seems to enforce 1 active activity + Dashboard.
             // We will stick to current finish() logic but add transitions.
             if (this !is DashboardActivity) {
                 finish()
             }
        }
        
        if (targetIndex > currentIndex) {
            // Moving Right -> Next screen slides in from Right
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            // Moving Left -> Next screen slides in from Left
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    protected fun startActivityWithTransition(intent: Intent, transitionType: TransitionType = TransitionType.SLIDE) {
        startActivity(intent)
        when (transitionType) {
            TransitionType.SLIDE -> overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            TransitionType.SLIDE_UP -> overridePendingTransition(R.anim.slide_up, R.anim.fade_out) // Fade out the background/previous activity slightly if needed, or just hold? Commonly R.anim.fade_out or no_anim. Let's use fade_out for smoother look or existing hold.
            // Actually, for Slide Up, the entering activity slides up. The exiting activity usually stays or fades.
            // But android.R.anim.fade_out or a custom "hold" animation is better.
            // Let's use generic fade out for the background activity for now.
            TransitionType.FADE -> overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            TransitionType.NONE -> overridePendingTransition(0, 0)
        }
    }

    protected fun setupToolbar(toolbar: com.google.android.material.appbar.MaterialToolbar, showHomeAsUp: Boolean = true) {
        if (showHomeAsUp) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black) // Ensure this icon exists or use standard
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }
    }

    protected fun showError(message: String) {
        val rootView = findViewById<android.view.View>(android.R.id.content)
        if (rootView != null) {
            com.google.android.material.snackbar.Snackbar.make(rootView, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setBackgroundTint(androidx.core.content.ContextCompat.getColor(this, R.color.vigilant_red))
                .setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
                .show()
        } else {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    enum class TransitionType {
        SLIDE, SLIDE_UP, FADE, NONE
    }
}
