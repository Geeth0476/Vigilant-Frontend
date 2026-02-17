package com.simats.vigilant

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import com.simats.vigilant.databinding.ActivityCreateAccountBinding
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreateAccountActivity : BaseActivity() {
    
    private lateinit var binding: ActivityCreateAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupTermsText()
        setupListeners()
        binding.tvPoweredBy.text = VigilantBrand.getPoweredByText(this)
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupListeners() {
        // Initial state
        binding.btnCreateAccount.alpha = 0.5f

        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnCreateAccount.isEnabled = isChecked
            binding.btnCreateAccount.alpha = if (isChecked) 1.0f else 0.5f
        }

        binding.btnCreateAccount.setOnClickListener {
             if (!binding.cbTerms.isChecked) {
                 Toast.makeText(this, "Please accept the Terms of Service to continue", Toast.LENGTH_SHORT).show()
                 return@setOnClickListener
             }

             val fullName = binding.etFullName.text.toString().trim()
             val email = binding.etEmail.text.toString().trim()
             val password = binding.etPassword.text.toString()
             val confirmPassword = binding.etConfirmPassword.text.toString()
             
             // Reset Errors
             binding.layoutFullName.error = null
             binding.layoutEmail.error = null
             binding.layoutPassword.error = null
             binding.layoutConfirmPassword.error = null
             
             var isValid = true
             
             if (fullName.isEmpty()) {
                 binding.layoutFullName.error = "Name is required"
                 isValid = false
             }
             
             if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                 binding.layoutEmail.error = "Enter a valid email"
                 isValid = false
             }
             
             if (password.length < 6) {
                 binding.layoutPassword.error = "Password must be at least 6 chars"
                 isValid = false
             }
             
             if (password != confirmPassword) {
                 binding.layoutConfirmPassword.error = "Passwords do not match"
                 isValid = false
             }
             


             if (isValid) {
                 // API Register
                  lifecycleScope.launch {
                     try {
                         binding.btnCreateAccount.isEnabled = false
                         val api = com.simats.vigilant.data.api.ApiClient.getService(this@CreateAccountActivity)
                         val response = api.register(com.simats.vigilant.data.model.RegisterRequest(email, password, fullName))
                         
                         if (response.isSuccessful && response.body()?.success == true) {
                             val data = response.body()?.data
                             if (data != null) {
                                  val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                                  com.simats.vigilant.data.TokenManager(this@CreateAccountActivity).saveInternalToken(data.access_token)
                                  
                                  // Save local profile info (partial)
                                  prefs.edit()
                                     .putBoolean("is_logged_in", false) // Not fully logged in yet
                                     .putString("temp_email", email) // Save for OTP
                                     .putString("profile_name", data.full_name)
                                     .apply()
                                     
                                  // Navigate to OTP
                                  val intent = android.content.Intent(this@CreateAccountActivity, OtpVerificationActivity::class.java)
                                  intent.putExtra("EMAIL", email)
                                  startActivityWithTransition(intent, TransitionType.FADE)
                                  finish()
                             }
                         } else {
                             // Try to parse error message from errorBody if available
                             var errorMsg = "Registration failed"
                             try {
                                 val errorBodyStr = response.errorBody()?.string()
                                 if (!errorBodyStr.isNullOrEmpty()) {
                                     // Simple attempt to find "message":"..."
                                     val json = org.json.JSONObject(errorBodyStr)
                                     if (json.has("error")) {
                                          val errObj = json.getJSONObject("error")
                                          if (errObj.has("message")) {
                                              errorMsg = errObj.getString("message")
                                          }
                                     }
                                 }
                             } catch (e: Exception) {
                                  android.util.Log.e("CreateAccount", "Error parsing error response", e)
                             }
                             showError(errorMsg)
                         }
                     } catch (e: Exception) {
                         val errorMessage = if (e is java.net.ConnectException || e is java.net.SocketTimeoutException) {
                             "Cannot reach server. Check Wi-Fi and Firewall."
                         } else {
                             "Network Error: ${e.message}"
                         }
                         showError(errorMessage)
                         e.printStackTrace()
                     } finally {
                         binding.btnCreateAccount.isEnabled = true
                     }
                  }
             }
        }

        binding.tvLogin.setOnClickListener {
             startActivityWithTransition(android.content.Intent(this, LoginActivity::class.java))
             finish()
        }
    }

    private fun setupTermsText() {
        val fullText = getString(R.string.terms_checkbox)
        val spannableString = SpannableString(fullText)

        val privacyPolicyText = "Privacy Policy"
        val termsOfServiceText = "Terms of Service"

        // Helper to add clickable span
        fun addClickableSpan(targetText: String) {
            val startIndex = fullText.indexOf(targetText)
            if (startIndex != -1) {
                val endIndex = startIndex + targetText.length
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Toast.makeText(this@CreateAccountActivity, "View $targetText", Toast.LENGTH_SHORT).show()
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(this@CreateAccountActivity, R.color.vigilant_blue)
                        ds.isUnderlineText = false
                    }
                }
                spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        addClickableSpan(privacyPolicyText)
        addClickableSpan(termsOfServiceText)

        binding.tvTerms.text = spannableString
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
    }
}
