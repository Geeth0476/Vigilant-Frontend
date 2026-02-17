package com.simats.vigilant

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class LegalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        setupAction(R.id.actionTos, R.drawable.ic_document_grey, getString(R.string.item_tos)) {
            // Open URL or Show Text
            Toast.makeText(this, "Opening Terms...", Toast.LENGTH_SHORT).show()
        }
        
        setupAction(R.id.actionPrivacyPolicy, R.drawable.ic_shield_grey, getString(R.string.item_privacy_policy)) {
             Toast.makeText(this, "Opening Privacy Policy...", Toast.LENGTH_SHORT).show()
        }
        
        setupAction(R.id.actionLicenses, R.drawable.ic_info_grey, getString(R.string.item_licenses)) {
             Toast.makeText(this, "Showing Licenses...", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupAction(includeId: Int, iconRes: Int, title: String, onClick: () -> Unit) {
        val view = findViewById<View>(includeId)
        view.findViewById<ImageView>(R.id.iconAction).setImageResource(iconRes)
        view.findViewById<TextView>(R.id.textActionTitle).text = title
        view.setOnClickListener { onClick() }
    }
}
