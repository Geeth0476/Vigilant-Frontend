package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ReportSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_success)
        
        findViewById<MaterialButton>(R.id.btnBackToCommunity).setOnClickListener {
             // "Back to Community" -> Community Feed
             val intent = Intent(this, CommunityFeedActivity::class.java)
             intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
             startActivity(intent)
             finish()
        }
        
        findViewById<MaterialButton>(R.id.btnViewReports).setOnClickListener {
            // "View Reports" -> My Reports
            val intent = Intent(this, ViewReportsActivity::class.java)
             intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
             startActivity(intent)
             finish()
        }
    }
}
