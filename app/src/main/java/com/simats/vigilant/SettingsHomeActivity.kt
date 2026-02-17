package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import android.widget.TextView
import android.widget.ImageView
import android.view.View

class SettingsHomeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_home)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        setupRow(R.id.setAccount, R.drawable.ic_person_grey, "Account", "Profile & security") {
            startActivityWithTransition(Intent(this, ProfileAccountActivity::class.java))
        }
        setupRow(R.id.setAppLock, R.drawable.ic_lock_grey, "App Lock", "PIN & Biometrics") {
             startActivityWithTransition(Intent(this, SecureAccessActivity::class.java))
        }
        setupRow(R.id.setScan, R.drawable.ic_shield_grey, "Scan & Detection", "Auto-scan, sensitivity") {
             startActivityWithTransition(Intent(this, ScanSettingsActivity::class.java))
        }
        setupRow(R.id.setNotify, R.drawable.ic_bell_outline_orange, "Alert Rules", "Alerts & quiet hours") { 
             startActivityWithTransition(Intent(this, AlertRulesActivity::class.java))
        }
        setupRow(R.id.setPrivacy, R.drawable.ic_lock_grey, "Privacy & Data", "Data usage & logs") {
             startActivityWithTransition(Intent(this, PrivacyDataActivity::class.java))
        }
        setupRow(R.id.setLegal, R.drawable.ic_document_grey, "Legal", "Terms & policies") {
             startActivityWithTransition(Intent(this, LegalActivity::class.java))
        }
        setupRow(R.id.setAbout, R.drawable.ic_info_grey, "About Vigilant", "") {
             startActivityWithTransition(Intent(this, AboutActivity::class.java))
        }
    }
    
    private fun setupRow(includeId: Int, iconRes: Int, title: String, subtitle: String, onClick: () -> Unit) {
        val view = findViewById<View>(includeId)
        view.findViewById<ImageView>(R.id.iconAction).setImageResource(iconRes)
        val tvTitle = view.findViewById<TextView>(R.id.textActionTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.textActionSubtitle)
        
        tvTitle.text = title
        if (subtitle.isNotEmpty()) {
            tvSubtitle.text = subtitle
            tvSubtitle.visibility = View.VISIBLE
        } else {
            tvSubtitle.visibility = View.GONE
        }
        view.setOnClickListener { onClick() }
    }
}
