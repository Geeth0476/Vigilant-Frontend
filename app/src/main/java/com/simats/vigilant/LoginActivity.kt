package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.simats.vigilant.databinding.ActivityLoginBinding
import com.simats.vigilant.ui.dashboard.DashboardActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        binding.tvPoweredBy.text = VigilantBrand.getPoweredByText(this)
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
             val email = binding.etEmail.text.toString().trim()
             val password = binding.etPassword.text.toString()
             
             // Reset Errors
             binding.layoutEmail.error = null
             binding.layoutPassword.error = null
             
             var isValid = true
             
             if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                 binding.layoutEmail.error = "Enter a valid email"
                 isValid = false
             }
             
             if (password.isEmpty()) {
                 binding.layoutPassword.error = "Password is required"
                 isValid = false
             }
             


             if (isValid) {
                 // API Login
                 lifecycleScope.launch {
                     try {
                         binding.btnLogin.isEnabled = false
                         val api = com.simats.vigilant.data.api.ApiClient.getService(this@LoginActivity)
                         
                         val deviceId = com.simats.vigilant.data.DeviceIdManager.getDeviceId(this@LoginActivity)
                         val model = android.os.Build.MODEL ?: "Unknown Android"
                         val os = "Android " + android.os.Build.VERSION.RELEASE
                         
                         val response = api.login(com.simats.vigilant.data.model.LoginRequest(email, password, deviceId, model, os))
                         
                         if (response.isSuccessful && response.body()?.success == true) {
                             val data = response.body()?.data
                             if (data != null) {
                                 val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                                 com.simats.vigilant.data.TokenManager(this@LoginActivity).saveInternalToken(data.access_token)
                                 loginSuccess(prefs, data, email)
                             } else {
                                 showError("Login failed: Unknown error")
                             }
                         } else {
                             // Api Error
                             val errorBodyStr = response.errorBody()?.string()
                             // Check if it is unverified
                             if (response.code() == 403 && errorBodyStr?.contains("UNVERIFIED") == true) {
                                 Toast.makeText(this@LoginActivity, "Please verify your email.", Toast.LENGTH_LONG).show()
                                 val intent = Intent(this@LoginActivity, OtpVerificationActivity::class.java)
                                 intent.putExtra("EMAIL", email)
                                 startActivityWithTransition(intent)
                             } else {
                                 val errorMsg = try {
                                     val json = org.json.JSONObject(errorBodyStr ?: "")
                                     json.getJSONObject("error").getString("message")
                                 } catch (e: Exception) {
                                     "Login failed"
                                 }
                                 showError(errorMsg)
                             }
                         }
                     } catch (e: Exception) {
                         showError("Network Error: ${e.message}")
                         e.printStackTrace()
                     } finally {
                         binding.btnLogin.isEnabled = true
                     }
                 }
             }
        }
        binding.tvForgotPassword.setOnClickListener {
             startActivityWithTransition(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvCreateAccount.setOnClickListener {
             val intent = Intent(this, CreateAccountActivity::class.java)
             startActivityWithTransition(intent)
             finish() 
        }
    }

    
    private fun loginSuccess(prefs: android.content.SharedPreferences, data: com.simats.vigilant.data.model.LoginData, email: String) {
         prefs.edit()
             .putBoolean("is_logged_in", true)
             .putString("profile_name", data.full_name)
             .putString("profile_email", email)
             .putString("profile_phone", data.phone)
             .putString("profile_image_uri", data.profile_image)
             .apply()
             
         Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
         
         // Redirect to PermissionRequestActivity to ensure permissions on this new device
         val intent = Intent(this, PermissionRequestActivity::class.java)
         intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
         startActivityWithTransition(intent, TransitionType.FADE)
    }
}
