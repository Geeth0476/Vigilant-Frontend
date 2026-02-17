package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import android.Manifest

class PermissionNotificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_notification)

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
        val btnEnable = findViewById<MaterialButton>(R.id.btnEnableNotifications)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnEnable.setOnClickListener {
             // Request permission code here (e.g., requestPermissions...)
             Toast.makeText(this, "Notification Permission Requested", Toast.LENGTH_SHORT).show()
             // Navigate to Step 3
             startActivity(Intent(this, PermissionAccessibilityActivity::class.java))
             finish()
        }

        tvSkip.setOnClickListener {
             // Navigate to Step 3
             startActivity(Intent(this, PermissionAccessibilityActivity::class.java))
             finish()
        }
    }
}
