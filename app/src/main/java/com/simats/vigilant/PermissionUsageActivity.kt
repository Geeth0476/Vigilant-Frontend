package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class PermissionUsageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_usage)

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
        val btnGrantPermission = findViewById<MaterialButton>(R.id.btnGrantPermission)
        val tvLearnMore = findViewById<TextView>(R.id.tvLearnMore)

        btnGrantPermission.setOnClickListener {
             // Skip actual permission check for demo flow
             startActivity(Intent(this, ScanIntroActivity::class.java))
             finish()
        }

        tvLearnMore.setOnClickListener {
             Toast.makeText(this, "Show detailed info dialog", Toast.LENGTH_SHORT).show()
        }
    }
}
