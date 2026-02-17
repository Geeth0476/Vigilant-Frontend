package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class LockScreenActivity : AppCompatActivity() {

    private val currentPin = StringBuilder()
    private val pinDots = arrayOfNulls<View>(4)
    private lateinit var tvPinDesc: TextView
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secure_access) // Reuse layout

        // Customize UI for Lock Screen
        findViewById<View>(R.id.topAppBar).visibility = View.GONE // Hide Back Button
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmPin).visibility = View.GONE // No confirm button needed for verify usually, or hide until full
        
        tvPinDesc = findViewById(R.id.tvPinDesc)
        findViewById<TextView>(R.id.tvPinTitle).text = "Vigilant Locked"
        tvPinDesc.text = "Enter your PIN to access"
        
        pinDots[0] = findViewById(R.id.pinDot1)
        pinDots[1] = findViewById(R.id.pinDot2)
        pinDots[2] = findViewById(R.id.pinDot3)
        pinDots[3] = findViewById(R.id.pinDot4)

        setupKeypad()
        
        // Biometrics
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("biometric_enabled", false)) {
            setupBiometric()
            biometricPrompt.authenticate(promptInfo)
        }
    }
    
    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockApp()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Fallback to PIN
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vigilant")
            .setSubtitle("Use your fingerprint or face")
            .setNegativeButtonText("Use PIN")
            .build()
    }

    private fun unlockApp() {
        if (!isTaskRoot) {
            finish()
            return
        }
        
        val intent = Intent(this, com.simats.vigilant.ui.dashboard.DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun verifyPin(pin: String) {
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        val savedPin = prefs.getString("app_pin", "")
        
        if (pin == savedPin) {
            unlockApp()
        } else {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            currentPin.clear()
            updatePinDots()
            // Shake animation could go here
        }
    }

    private fun setupKeypad() {
        val listener = View.OnClickListener { v ->
            if (v is TextView) {
                val digit = v.text.toString()
                appendDigit(digit)
            }
        }

        findViewById<TextView>(R.id.btnKey1).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey2).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey3).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey4).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey5).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey6).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey7).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey8).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey9).setOnClickListener(listener)
        findViewById<TextView>(R.id.btnKey0).setOnClickListener(listener)

        findViewById<ImageView>(R.id.btnKeyBackspace).setOnClickListener {
            removeDigit()
        }
    }

    private fun appendDigit(digit: String) {
        if (currentPin.length < 4) {
            currentPin.append(digit)
            updatePinDots()
            
            if (currentPin.length == 4) {
                // Auto-verify on 4th digit
                verifyPin(currentPin.toString())
            }
        }
    }

    private fun removeDigit() {
        if (currentPin.isNotEmpty()) {
            currentPin.deleteCharAt(currentPin.length - 1)
            updatePinDots()
        }
    }

    private fun updatePinDots() {
        val length = currentPin.length
        for (i in 0 until 4) {
             if (i < length) {
                 pinDots[i]?.setBackgroundResource(R.drawable.bg_pin_indicator_filled)
             } else {
                 pinDots[i]?.setBackgroundResource(R.drawable.bg_pin_indicator_empty)
             }
        }
    }
    
    // Disable Back Button to prevent bypass
    override fun onBackPressed() {
        // Move task to back instead of finishing/bypassing
        moveTaskToBack(true)
    }
}
