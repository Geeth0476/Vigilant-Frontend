package com.simats.vigilant

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import android.graphics.Color

class ReportDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_detail)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Get Data
        val appName = intent.getStringExtra("APP_NAME") ?: "Unknown App"
        val reportType = intent.getStringExtra("REPORT_TYPE") ?: "General"
        val reportDate = intent.getStringExtra("REPORT_DATE") ?: "Unknown Date"
        val reportStatus = intent.getStringExtra("REPORT_STATUS") ?: "Pending"

        // Bind Data
        findViewById<TextView>(R.id.tvAppName).text = appName
        findViewById<TextView>(R.id.tvReportType).text = reportType
        findViewById<TextView>(R.id.tvReportDate).text = "Reported on $reportDate"
        
        val tvStatus = findViewById<TextView>(R.id.tvReportStatus)
        tvStatus.text = reportStatus
        
        if (reportStatus == "Resolved") {
             tvStatus.setTextColor(Color.parseColor("#4CAF50"))
             findViewById<TextView>(R.id.tvStatusDescription).text = "This threat has been verified and added to the community database. Thank you for your contribution!"
        } else {
             tvStatus.setTextColor(Color.parseColor("#F57C00"))
             findViewById<TextView>(R.id.tvStatusDescription).text = "Your report is waiting for approval from our security team. Once approved, it will be visible in the Community Feed."
        }
    }
}
