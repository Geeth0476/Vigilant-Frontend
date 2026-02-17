package com.simats.vigilant

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        // Apply Brand Styling to App Name
        val tvAppName = findViewById<android.widget.TextView>(R.id.tvAboutAppName)
        tvAppName.text = VigilantBrand.getStyledLogo(this)
        
        val tvPoweredBy = findViewById<android.widget.TextView>(R.id.tvPoweredBy)
        tvPoweredBy.text = VigilantBrand.getPoweredByText(this)

        // Hidden Interaction: Double Tap on App Logo
        val ivAppLogo = findViewById<android.widget.ImageView>(R.id.ivAppLogo)
        val gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: android.view.MotionEvent): Boolean {
                return true
            }

            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                showDeveloperDetails()
                return true
            }
        })

        ivAppLogo.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        // Contact Email Click
        val tvContact = findViewById<android.widget.TextView>(R.id.tvContact)
        tvContact.setOnClickListener {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO)
            intent.data = android.net.Uri.parse("mailto:vigilantappdetection@gmail.com")
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Vigilant App Support")
            try {
                startActivity(android.content.Intent.createChooser(intent, "Send Email"))
            } catch (e: Exception) {
                // Ignore if no email app
            }
        }
    }

    private fun showDeveloperDetails() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Developer Details")
            .setMessage("Developed by Goli Geeth\ngeethnanigoli@gmail.com")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
