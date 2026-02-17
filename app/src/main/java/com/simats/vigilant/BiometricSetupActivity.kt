package com.simats.vigilant

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.util.concurrent.Executor

class BiometricSetupActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometric_setup)

        setupToolbar()
        setupBiometric()
        setupListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupBiometric() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext,
                        "Biometric setup successful!", Toast.LENGTH_SHORT)
                        .show()
                    
                    // Save preference
                    getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("biometric_enabled", true)
                        .apply()
                        
                    navigateToPermissions()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Vigilant")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()
    }

    private fun setupListeners() {
        val btnEnableBiometrics = findViewById<MaterialButton>(R.id.btnEnableBiometrics)
        val tvSkip = findViewById<TextView>(R.id.tvSkip)

        btnEnableBiometrics.setOnClickListener {
             biometricPrompt.authenticate(promptInfo)
        }

        tvSkip.setOnClickListener {
             Toast.makeText(this, "Skipped Biometrics", Toast.LENGTH_SHORT).show()
             getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("biometric_enabled", false)
                        .apply()
             navigateToPermissions()
        }
    }

    private fun navigateToPermissions() {
        if (intent.getBooleanExtra("IS_ONBOARDING", false)) {
            startActivity(android.content.Intent(this, PermissionRequestActivity::class.java))
        }
        finish()
    }
}
