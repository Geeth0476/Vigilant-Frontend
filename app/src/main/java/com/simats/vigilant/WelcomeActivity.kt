package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simats.vigilant.databinding.ActivityWelcomeBinding
import com.simats.vigilant.ui.dashboard.DashboardActivity

class WelcomeActivity : BaseActivity() {
    
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivityWithTransition(Intent(this, DashboardActivity::class.java), TransitionType.FADE)
            finish()
            return
        }

        // Apply Brand Styling to Title
        // "Welcome to " + Brand
        val prefix = "Welcome to "
        val brandSpan = VigilantBrand.getStyledLogo(this)
        val finalSpan = android.text.SpannableStringBuilder(prefix)
        finalSpan.append(brandSpan)
        binding.tvTitle.text = finalSpan

        // Set click listeners
        // Set click listeners
        binding.btnCreateAccount.setOnClickListener {
            // Navigate to Subscription -> Create Account
            val intent = Intent(this, SubscriptionActivity::class.java)
            intent.putExtra("NEXT_SCREEN", "REGISTER")
            startActivityWithTransition(intent)
        }

        binding.btnLogin.setOnClickListener {
            // Navigate to Subscription -> Login
            val intent = Intent(this, SubscriptionActivity::class.java)
            intent.putExtra("NEXT_SCREEN", "LOGIN")
            startActivityWithTransition(intent)
        }

        binding.tvGuest.setOnClickListener {
            startActivityWithTransition(Intent(this, GuestModeActivity::class.java))
        }
        
        binding.tvPoweredBy.text = VigilantBrand.getPoweredByText(this)
    }
}