package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ResetPasswordActivity : BaseActivity() {

    private var userEmail: String? = null
    private var resendTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        userEmail = intent.getStringExtra("EMAIL")

        setupToolbar()
        setupListeners()
        startResendTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }

    private fun startResendTimer() {
        val btnResend = findViewById<MaterialButton>(R.id.btnResendOtp)
        btnResend.isEnabled = false
        
        resendTimer?.cancel()
        resendTimer = object : android.os.CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                btnResend.text = String.format("Resend Code (%d:%02d)", minutes, remainingSeconds)
            }

            override fun onFinish() {
                btnResend.isEnabled = true
                btnResend.text = "Resend Code"
            }
        }.start()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupListeners() {
        val btnReset = findViewById<MaterialButton>(R.id.btnResetPassword)
        val etOtp = findViewById<TextInputEditText>(R.id.etOtp)
        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirm = findViewById<TextInputEditText>(R.id.etConfirmPassword)

        btnReset.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            val newPass = etNew.text.toString()
            val confirmPass = etConfirm.text.toString()

            if (userEmail.isNullOrEmpty()) {
                showError("Error: Email not found")
                return@setOnClickListener
            }

            if (otp.length != 6) {
                showError("Invalid OTP")
                return@setOnClickListener
            }

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                showError("Please enter new password")
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                showError("Passwords do not match")
                return@setOnClickListener
            }

            resetPassword(userEmail!!, otp, newPass)
        }
        
        val btnResend = findViewById<MaterialButton>(R.id.btnResendOtp)
        btnResend.setOnClickListener {
            userEmail?.let { email ->
                resendOtp(email)
            }
        }
    }
    
    private fun resendOtp(email: String) {
        lifecycleScope.launch {
            try {
                // showLoading? maybe just disable resend button
                val btnResend = findViewById<MaterialButton>(R.id.btnResendOtp)
                btnResend.isEnabled = false
                btnResend.text = "Sending..."
                
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@ResetPasswordActivity)
                val response = api.forgotPassword(mapOf("email" to email))
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ResetPasswordActivity, "OTP Resent!", Toast.LENGTH_SHORT).show()
                    startResendTimer()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: ""
                    if (response.code() == 429) {
                         Toast.makeText(this@ResetPasswordActivity, "Please wait before resending.", Toast.LENGTH_SHORT).show()
                         // Optionally parse seconds and restart timer with remaining time
                         startResendTimer() // restart full timer for safety or user penalty
                    } else {
                         Toast.makeText(this@ResetPasswordActivity, "Failed to resend OTP.", Toast.LENGTH_SHORT).show()
                         btnResend.isEnabled = true
                         btnResend.text = "Resend Code"
                    }
                }
            } catch (e: Exception) {
                 Toast.makeText(this@ResetPasswordActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                 findViewById<MaterialButton>(R.id.btnResendOtp).isEnabled = true
                 findViewById<MaterialButton>(R.id.btnResendOtp).text = "Resend Code"
            }
        }
    }

    private fun resetPassword(email: String, otp: String, pass: String) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@ResetPasswordActivity)
                val response = api.resetPassword(mapOf("email" to email, "otp" to otp, "new_password" to pass))

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ResetPasswordActivity, "Password reset successfully. Please login.", Toast.LENGTH_LONG).show()
                    // Navigate to LoginActivity and clear back stack
                    val intent = android.content.Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to reset password"
                    showError("Reset failed. Invalid OTP or Email.")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        findViewById<MaterialButton>(R.id.btnResetPassword).isEnabled = !isLoading
        findViewById<MaterialButton>(R.id.btnResetPassword).text = if (isLoading) "Resetting..." else "Reset Password"
    }
}
