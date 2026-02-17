package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class ManagePermissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_permissions)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        findViewById<MaterialButton>(R.id.btnOpenSettings).setOnClickListener {
             // Ideally open specific app details settings
             val intent = Intent(Settings.ACTION_SETTINGS)
             startActivity(intent)
        }
        
        findViewById<MaterialButton>(R.id.btnRescan).setOnClickListener {
             Toast.makeText(this, "Scanning app...", Toast.LENGTH_SHORT).show()
        }
    }
}
