package com.simats.vigilant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class SecuritySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_settings)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        // App Lock - Navigate to Secure Access (PIN setup)
        setupRow(
            R.id.optAppLock, 
            R.drawable.ic_lock_grey, 
            "App Lock",
            "(PIN & Biometrics)"
        ) {
            startActivity(android.content.Intent(this, SecureAccessActivity::class.java))
        }
        
        // Biometrics - Navigate to Biometric Setup
        setupRow(
            R.id.optBiometrics, 
            R.drawable.ic_fingerprint_grey, 
            "Biometric Authentication",
            "Fingerprint or face unlock"
        ) {
            startActivity(android.content.Intent(this, BiometricSetupActivity::class.java))
        }
        
        // Change PIN - Navigate to Secure Access
        setupRow(
            R.id.optChangePin, 
            R.drawable.ic_pin_grey, 
            "Change App PIN",
            "Update your security PIN"
        ) {
            startActivity(android.content.Intent(this, SecureAccessActivity::class.java))
        }

        // Change Password - Navigate to ChangePasswordActivity
        setupRow(
            R.id.optChangePassword, 
            R.drawable.ic_lock_grey, // Using lock icon again or new one if available
            "Change Account Password",
            "Update your account password"
        ) {
            startActivity(android.content.Intent(this, ChangePasswordActivity::class.java))
        }

    }

    private fun setupRow(includeId: Int, iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
        val view = findViewById<android.view.View>(includeId)
        view.findViewById<android.widget.ImageView>(R.id.iconAction).setImageResource(iconRes)
        view.findViewById<android.widget.TextView>(R.id.textActionTitle).text = "$title\n$subtitle"
        
        view.setOnClickListener {
            onClick()
        }
    }
}
