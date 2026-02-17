package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PermissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        setupListeners()
    }

    private fun setupListeners() {
        val btnContinuePerm = findViewById<MaterialButton>(R.id.btnContinuePerm)

        btnContinuePerm.setOnClickListener {
             // Skip detailed permission flow as requested, go straight to Scan Intro
             startActivity(Intent(this, ScanIntroActivity::class.java))
        }
    }
}
