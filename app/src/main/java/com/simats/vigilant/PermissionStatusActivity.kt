package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class PermissionStatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_status)

        setupListeners()
    }

    private fun setupListeners() {
        val btnContinue = findViewById<MaterialButton>(R.id.btnFinalContinue)

        btnContinue.setOnClickListener {
             // Navigate to Scan Intro
             val intent = Intent(this, ScanIntroActivity::class.java)
             startActivity(intent)
             finish()
        }
    }
}
