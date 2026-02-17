package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.vigilant.ui.scan.ScanProgressActivity

class ScanIntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_intro)

        setupListeners()
    }

    private fun setupListeners() {
        val btnStart = findViewById<MaterialButton>(R.id.btnStartScan)

        btnStart.setOnClickListener {
             Toast.makeText(this, "Starting Initial Scan...", Toast.LENGTH_SHORT).show()
             // Navigate to Scan Progress (Detailed/Initial)
             val intent = Intent(this, ScanProgressActivity::class.java)
             intent.putExtra("IS_DEEP_SCAN", true)
             startActivity(intent)
             finish()
        }
    }
}
