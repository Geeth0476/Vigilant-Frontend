package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.simats.vigilant.databinding.ActivityOtpVerificationBinding

class OtpVerificationActivity : BaseActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userEmail = intent.getStringExtra("EMAIL")
        if (userEmail != null) {
            binding.tvInstructions.text = "We have sent the verification code to $userEmail"
        }

        binding.topAppBar.setNavigationOnClickListener { finish() }

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6) {
                 binding.layoutOtp.error = "Please enter valid 6-digit code"
            } else {
                 binding.layoutOtp.error = null
                 verifyOtp(otp)
            }
        }

        binding.tvResend.setOnClickListener {
            resendOtp()
        }
    }

    private fun verifyOtp(otp: String) {
        lifecycleScope.launch {
            try {
                binding.btnVerify.isEnabled = false
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@OtpVerificationActivity)
                val response = api.verifyOtp(mapOf("otp" to otp))

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@OtpVerificationActivity, "Verified Successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Finalize Login
                    val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                    prefs.edit()
                         .putBoolean("is_logged_in", true)
                         .putString("profile_email", userEmail)
                         .apply()

                    startActivityWithTransition(Intent(this@OtpVerificationActivity, AccountSuccessActivity::class.java), TransitionType.FADE)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("OTP", "Error: $errorBody")
                    showError("Invalid OTP. PLease check otp_logs.txt on server.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("Network error: ${e.message}")
            } finally {
                binding.btnVerify.isEnabled = true
            }
        }
    }

    private fun resendOtp() {
        lifecycleScope.launch {
            try {
                binding.tvResend.isEnabled = false
                binding.tvResend.alpha = 0.5f
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@OtpVerificationActivity)
                val response = api.resendOtp()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@OtpVerificationActivity, "Code resent! Check email/logs.", Toast.LENGTH_LONG).show()
                } else {
                    showError("Failed to resend code.")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            } finally {
                binding.tvResend.isEnabled = true
                binding.tvResend.alpha = 1.0f
            }
        }
    }
}
