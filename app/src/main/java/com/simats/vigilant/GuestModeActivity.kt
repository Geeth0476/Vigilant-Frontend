package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.simats.vigilant.ui.dashboard.DashboardActivity

class GuestModeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guest_mode)

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
        val btnContinueGuest = findViewById<MaterialButton>(R.id.btnContinueGuest)
        val tvCreateAccount = findViewById<TextView>(R.id.tvCreateAccount)

        btnContinueGuest.setOnClickListener {
             Toast.makeText(this, "Entering Guest Mode...", Toast.LENGTH_SHORT).show()
             // Navigate to Dashboard in Guest Mode
             val intent = Intent(this, DashboardActivity::class.java)
             intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
             startActivity(intent)
        }

        tvCreateAccount.setOnClickListener {
             // Navigate to Create Account, close this and welcome? 
             // Simpler to just start CreateAccount
             startActivity(Intent(this, CreateAccountActivity::class.java))
             finish() 
        }
    }
}
