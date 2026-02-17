package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class PermissionAccessibilityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_accessibility)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupListeners() {
        val btnGrant = findViewById<MaterialButton>(R.id.btnGrantAcc)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnGrant.setOnClickListener {
             // Open Accessibility Settings
             try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                // Optimistically navigate to status or wait for back
                // For demo:
                // startActivity(Intent(this, PermissionStatusActivity::class.java))
                // finish()
             } catch (e: Exception) {
                 Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
             }
             // Forcing navigation for demo flow
             startActivity(Intent(this, PermissionStatusActivity::class.java))
             finish()
        }

        tvSkip.setOnClickListener {
             // Finalize onboarding, go to Status
             startActivity(Intent(this, PermissionStatusActivity::class.java))
             finish()
        }
    }
}
