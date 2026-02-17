package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class CommunityThreatDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_threat_detail)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }
        
        val threatId = intent.getStringExtra("THREAT_ID")
        val appNameFallback = intent.getStringExtra("APP_NAME")
        
        val threat = if (threatId != null) {
            CommunityRepository.getThreatById(threatId)
        } else if (appNameFallback != null) {
            CommunityRepository.getThreatByName(appNameFallback)
        } else {
            null
        }
        
        if (threat == null) {
            Toast.makeText(this, "Threat details not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        topAppBar.title = threat.appName
        
        // Bind Header
        findViewById<android.widget.TextView>(R.id.tvAppName).text = threat.appName
        findViewById<android.widget.TextView>(R.id.tvCategory).text = threat.category
        
        // Risk Styling
        val tvRiskLevel = findViewById<android.widget.TextView>(R.id.tvRiskLevel)
        val llRiskPill = findViewById<android.widget.LinearLayout>(R.id.llRiskPill)
        val flIconContainer = findViewById<android.widget.FrameLayout>(R.id.flIconContainer)
        val ivAppIcon = findViewById<android.widget.ImageView>(R.id.ivAppIcon)
        
        tvRiskLevel.text = threat.riskLevel
        
        // Colors
        val (color, bgColor, iconRes) = when(threat.riskLevel.uppercase()) {
            "CRITICAL" -> Triple(R.color.vigilant_red, "#FFEBEE", R.drawable.ic_alert_red) // bg_square_soft_red
            "HIGH" -> Triple(R.color.vigilant_red, "#FFEBEE", R.drawable.ic_warning_amber) // Reusing amber/red
            "MEDIUM" -> Triple(R.color.vigilant_amber, "#FFF8E1", R.drawable.ic_warning_amber)
            else -> Triple(R.color.vigilant_green, "#E8F5E9", R.drawable.ic_check_circle_green)
        }
        
        tvRiskLevel.setTextColor(androidx.core.content.ContextCompat.getColor(this, color))
        llRiskPill.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(bgColor))
        
        flIconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(bgColor))
        ivAppIcon.setImageResource(iconRes)
        
        // Stats
        findViewById<android.widget.TextView>(R.id.tvReportCount).text = threat.reportCount.toString()
        findViewById<android.widget.TextView>(R.id.tvFirstSeen).text = threat.firstSeen
        findViewById<android.widget.TextView>(R.id.tvLastReported).text = threat.lastReported
        
        // Description
        findViewById<android.widget.TextView>(R.id.tvDescription).text = threat.description
        
        // Behaviors
        val behaviorsContainer = findViewById<android.widget.LinearLayout>(R.id.llBehaviorsContainer)
        behaviorsContainer.removeAllViews()
        
        threat.behaviors.forEach { behavior ->
            addBehaviorCard(behaviorsContainer, behavior)
        }
        
        // Actions
        findViewById<MaterialButton>(R.id.btnCheckDevice).setOnClickListener {
             // Logic to check currently installed apps for this package
             try {
                 packageManager.getPackageInfo(threat.packageName, 0)
                 // Found! Go to analysis
                 val intent = Intent(this, AppAnalysisActivity::class.java)
                 intent.putExtra("PACKAGE_NAME", threat.packageName)
                 startActivity(intent)
             } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                 Toast.makeText(this, "App not found on this device.", Toast.LENGTH_SHORT).show()
             }
        }
        
        findViewById<MaterialButton>(R.id.btnReportApp).setOnClickListener {
             val intent = Intent(this, ReportAppActivity::class.java)
             // Pre-fill? optional
             startActivity(intent)
        }
    }
    
    private fun addBehaviorCard(container: android.widget.LinearLayout, title: String) {
        val cv = com.google.android.material.card.MaterialCardView(this)
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, 16)
        cv.layoutParams = params
        cv.radius = 24f // 12dp approx? 36px? Let's use simple float
        cv.cardElevation = 0f
        cv.strokeWidth = 2 // 1dp approx
        cv.setStrokeColor(android.graphics.Color.parseColor("#EEEEEE"))
        cv.setCardBackgroundColor(android.graphics.Color.WHITE)
        
        val ll = android.widget.LinearLayout(this)
        ll.orientation = android.widget.LinearLayout.HORIZONTAL
        ll.setPadding(40, 40, 40, 40) // approx 16dp
        ll.gravity = android.view.Gravity.CENTER_VERTICAL
        
        val iv = android.widget.ImageView(this)
        iv.setImageResource(R.drawable.ic_info_outline) // Generic behavior icon
        // Ideally map behavior type to icon, but let's use generic for now
        iv.layoutParams = android.widget.LinearLayout.LayoutParams(60, 60) // 24dp
        (iv.layoutParams as android.widget.LinearLayout.LayoutParams).marginEnd = 40
        
        val tv = android.widget.TextView(this)
        tv.text = title
        tv.setTextColor(android.graphics.Color.BLACK)
        tv.textSize = 15f
        
        ll.addView(iv)
        ll.addView(tv)
        cv.addView(ll)
        
        container.addView(cv)
    }
}
