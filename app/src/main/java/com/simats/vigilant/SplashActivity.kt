package com.simats.vigilant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.simats.vigilant.ui.dashboard.DashboardActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize views
        // Initialize views
        val shield = findViewById<android.widget.ImageView>(R.id.splash_shield)
        val arrow = findViewById<android.widget.ImageView>(R.id.splash_arrow)
        val ring = findViewById<android.view.View>(R.id.splash_ring)
        val text = findViewById<android.widget.TextView>(R.id.splash_text)

        // Setup Text Color (Blue 'i's)
        // Setup Text Color (Blue Dots)
        text.text = VigilantBrand.getStyledLogo(this)
        
        // Initial States
        shield.alpha = 0f
        shield.scaleX = 0.9f
        shield.scaleY = 0.9f
        
        arrow.alpha = 0f
        arrow.scaleX = 0.9f
        arrow.scaleY = 0.9f
        
        ring.alpha = 0f
        ring.scaleX = 0.8f
        ring.scaleY = 0.8f
        
        text.alpha = 0f
        text.translationY = 50f

        // Animators
        // Animators
        val mediumDuration = resources.getInteger(R.integer.anim_duration_medium).toLong()
        val longDuration = resources.getInteger(R.integer.anim_duration_long).toLong()
        val xlDuration = resources.getInteger(R.integer.anim_duration_xl).toLong()

        // 1. Shield & Arrow Entry (0-600)
        val shieldFade = android.animation.ObjectAnimator.ofFloat(shield, "alpha", 0f, 1f).setDuration(longDuration)
        val shieldScaleX = android.animation.ObjectAnimator.ofFloat(shield, "scaleX", 0.9f, 1f).setDuration(longDuration)
        val shieldScaleY = android.animation.ObjectAnimator.ofFloat(shield, "scaleY", 0.9f, 1f).setDuration(longDuration)
        shieldScaleX.interpolator = android.view.animation.DecelerateInterpolator()
        shieldScaleY.interpolator = android.view.animation.DecelerateInterpolator()
        
        val arrowFade = android.animation.ObjectAnimator.ofFloat(arrow, "alpha", 0f, 1f).setDuration(longDuration)
        val arrowScaleX = android.animation.ObjectAnimator.ofFloat(arrow, "scaleX", 0.9f, 1f).setDuration(longDuration)
        val arrowScaleY = android.animation.ObjectAnimator.ofFloat(arrow, "scaleY", 0.9f, 1f).setDuration(longDuration)
        
        // 2. Arrow Pulse (600-1600) - Start after 600
        val arrowPulseScaleX = android.animation.ObjectAnimator.ofFloat(arrow, "scaleX", 1f, 1.1f, 1f).setDuration(xlDuration)
        val arrowPulseScaleY = android.animation.ObjectAnimator.ofFloat(arrow, "scaleY", 1f, 1.1f, 1f).setDuration(xlDuration)
        arrowPulseScaleX.startDelay = longDuration
        arrowPulseScaleY.startDelay = longDuration
        
        // 3. Ring Ripple (1600-2200) - Start after 1600
        val rippleStart = longDuration + xlDuration
        val ringFadeIn = android.animation.ObjectAnimator.ofFloat(ring, "alpha", 0f, 1f).setDuration(300)
        val ringFadeOut = android.animation.ObjectAnimator.ofFloat(ring, "alpha", 1f, 0f).setDuration(300)
        val ringScaleX = android.animation.ObjectAnimator.ofFloat(ring, "scaleX", 0.8f, 1.4f).setDuration(longDuration)
        val ringScaleY = android.animation.ObjectAnimator.ofFloat(ring, "scaleY", 0.8f, 1.4f).setDuration(longDuration)
        ringFadeIn.startDelay = rippleStart
        ringFadeOut.startDelay = rippleStart + 300
        ringScaleX.startDelay = rippleStart
        ringScaleY.startDelay = rippleStart

        // 4. Text Entry (2200-2600)
        val textStart = rippleStart + longDuration
        val textFade = android.animation.ObjectAnimator.ofFloat(text, "alpha", 0f, 1f).setDuration(mediumDuration)
        val textSlide = android.animation.ObjectAnimator.ofFloat(text, "translationY", 50f, 0f).setDuration(mediumDuration)
        textFade.startDelay = textStart
        textSlide.startDelay = textStart
        
        val animatorSet = android.animation.AnimatorSet()
        animatorSet.playTogether(
            shieldFade, shieldScaleX, shieldScaleY,
            arrowFade, arrowScaleX, arrowScaleY,
            arrowPulseScaleX, arrowPulseScaleY,
            ringFadeIn, ringFadeOut, ringScaleX, ringScaleY,
            textFade, textSlide
        )
        animatorSet.start()

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("vigilant_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)

            if (isLoggedIn) {
                // Check for App Lock
                val savedPin = prefs.getString("app_pin", "")
                if (!savedPin.isNullOrEmpty()) {
                    startActivity(Intent(this, LockScreenActivity::class.java))
                } else {
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                startActivity(Intent(this, WelcomeActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            finish()
        }, resources.getInteger(R.integer.splash_screen_duration).toLong())
    }
}
