package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AccountSuccessActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_success)

        val btnContinue = findViewById<MaterialButton>(R.id.btnContinue)
        val contentContainer = findViewById<android.widget.LinearLayout>(R.id.contentContainer)

        // Entry Animation
        contentContainer.alpha = 0f
        contentContainer.translationY = 100f

        contentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        btnContinue.setOnClickListener {
            val intent = android.content.Intent(this, SecureAccessActivity::class.java)
            intent.putExtra("IS_ONBOARDING", true)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }
}
