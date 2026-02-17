package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AccountActionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_actions)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        // Logout action
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardLogout).setOnClickListener {
            // Clear any session data and navigate to Welcome screen
            
            getSharedPreferences("vigilant_prefs", MODE_PRIVATE).edit().clear().apply()
            
            val intent = android.content.Intent(this, WelcomeActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        
        setupRow(R.id.actionReset, R.drawable.ic_security_update_amber, "Reset Security Setup", "Reset PIN & Biometrics") {
             Toast.makeText(this, "Reset initiated", Toast.LENGTH_SHORT).show()
        }
        
        setupRow(R.id.actionSignOutAll, R.drawable.ic_logout_red, "Sign Out of All Devices", "Manage active sessions") {
             startActivity(android.content.Intent(this, ActiveSessionsActivity::class.java))
        }
        
        setupRow(R.id.actionDelete, R.drawable.ic_delete_red, "Delete Account", "Permanently delete account and data") {
             // Show confirmation dialog logic would go here
             // For now, simulate with Toast
             Toast.makeText(this, "Please contact support to delete account.", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupRow(includeId: Int, iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
        val view = findViewById<android.view.View>(includeId)
        view.findViewById<android.widget.ImageView>(R.id.iconAction).apply {
            setImageResource(iconRes)
        }
        view.findViewById<android.widget.TextView>(R.id.textActionTitle).text = "$title\n$subtitle"
        
        view.setOnClickListener {
            onClick()
        }
    }
}
