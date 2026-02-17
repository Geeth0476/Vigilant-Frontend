package com.simats.vigilant

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class AlertDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_detail)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Get alert from intent
        @Suppress("DEPRECATION")
        val alert = intent.getParcelableExtra<SecurityAlert>("alert")
        
        if (alert != null) {
            displayAlert(alert)
        } else {
            finish()
        }
    }

    private fun displayAlert(alert: SecurityAlert) {
        // Title and time
        findViewById<TextView>(R.id.tvAlertTitle).text = alert.title
        findViewById<TextView>(R.id.tvAlertTime).text = "Detected ${getTimeAgo(alert.timestamp)}"

        // Severity
        val severityIndicator = findViewById<View>(R.id.severityIndicator)
        val tvSeverity = findViewById<TextView>(R.id.tvSeverity)
        
        when (alert.severity) {
            SecurityAlert.Severity.HIGH -> {
                severityIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.vigilant_red)
                tvSeverity.text = "HIGH RISK"
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.vigilant_red))
            }
            SecurityAlert.Severity.MEDIUM -> {
                severityIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.vigilant_orange)
                tvSeverity.text = "MEDIUM RISK"
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.vigilant_orange))
            }
            SecurityAlert.Severity.LOW -> {
                severityIndicator.backgroundTintList = ContextCompat.getColorStateList(this, R.color.vigilant_success_green)
                tvSeverity.text = "LOW RISK"
                tvSeverity.setTextColor(ContextCompat.getColor(this, R.color.vigilant_success_green))
            }
        }

        // Description and detailed info
        findViewById<TextView>(R.id.tvAlertDescription).text = alert.description
        findViewById<TextView>(R.id.tvDetailedInfo).text = alert.detailedInfo

        // Recommendations
        val recommendationsContainer = findViewById<LinearLayout>(R.id.recommendationsContainer)
        alert.recommendations.forEachIndexed { index, recommendation ->
            val textView = TextView(this).apply {
                text = "${index + 1}. $recommendation"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@AlertDetailActivity, R.color.vigilant_text_secondary))
                setPadding(0, 0, 0, dpToPx(12))
                setLineSpacing(dpToPx(4).toFloat(), 1.0f)
            }
            recommendationsContainer.addView(textView)
        }

        // Buttons
        findViewById<MaterialButton>(R.id.btnDismiss).setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.btnTakeAction).setOnClickListener {
            // Navigate to app settings or permissions
            finish()
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            seconds < 60 -> "$seconds seconds ago"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            else -> "${hours / 24} days ago"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
