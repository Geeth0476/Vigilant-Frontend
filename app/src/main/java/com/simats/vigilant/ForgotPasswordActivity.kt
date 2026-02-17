package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        setupToolbar()
        setupListeners()
        val tvPoweredBy = findViewById<android.widget.TextView>(R.id.tvPoweredBy)
        tvPoweredBy.text = VigilantBrand.getPoweredByText(this)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupListeners() {
        val btnSendLink = findViewById<MaterialButton>(R.id.btnSendLink)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)

        btnSendLink.setOnClickListener {
             val email = etEmail.text.toString().trim()
             if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                 sendOtp(email)
             } else {
                 etEmail.error = "Please enter a valid email"
             }
        }
    }

    private fun sendOtp(email: String) {
        showLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@ForgotPasswordActivity)
                val response = api.forgotPassword(mapOf("email" to email))

                if (response.isSuccessful && response.body()?.success == true) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(this@ForgotPasswordActivity, "OTP sent to $email", Toast.LENGTH_SHORT).show()
                        val intent = android.content.Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("EMAIL", email)
                        startActivity(intent)
                        finish()
                    }
                } else {
                     kotlinx.coroutines.withContext(Dispatchers.Main) {
                        Toast.makeText(this@ForgotPasswordActivity, "Failed to send OTP. Email may not exist.", Toast.LENGTH_SHORT).show()
                     }
                }
            } catch (e: Exception) {
                 kotlinx.coroutines.withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                 }
            } finally {
                 kotlinx.coroutines.withContext(Dispatchers.Main) {
                    showLoading(false)
                 }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        val btn = findViewById<MaterialButton>(R.id.btnSendLink)
        btn.isEnabled = !isLoading
        btn.text = if (isLoading) "Sending..." else "Send Reset Link"
    }
}
