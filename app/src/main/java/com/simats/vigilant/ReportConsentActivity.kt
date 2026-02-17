package com.simats.vigilant

import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ReportConsentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_consent)
        
        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)
        val checkbox = findViewById<CheckBox>(R.id.checkboxConsent)
        
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            btnContinue.isEnabled = isChecked
        }
        
        btnContinue.setOnClickListener {
             val intent = android.content.Intent(this, ReportAdditionalDetailsActivity::class.java)
             intent.putExtras(getIntent())
             startActivity(intent)
        }
    }
}
