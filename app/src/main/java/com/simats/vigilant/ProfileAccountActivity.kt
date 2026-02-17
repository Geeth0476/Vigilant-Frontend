package com.simats.vigilant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.vigilant.databinding.ActivityProfileAccountBinding
import com.simats.vigilant.databinding.ItemProfileActionBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileAccountActivity : BaseActivity() {
    
    private lateinit var binding: ActivityProfileAccountBinding
    
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Update UI with new data
            loadProfileData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Header Click Listener
        binding.llProfileHeader.setOnClickListener {
            // Navigate to View Profile now
            startActivity(Intent(this, ViewProfileActivity::class.java))
        }
        
        // Load profile data
        loadProfileData()
        
        // Setup Bottom Navigation
        setupBottomNavigation(binding.bottomNavigation, R.id.nav_profile)
        
        // Setup Actions
        setupAction(binding.actionEditProfile, R.drawable.ic_person_grey, "Edit Profile") {
            editProfileLauncher.launch(Intent(this, EditProfileActivity::class.java))
        }

        setupAction(binding.actionMyReports, R.drawable.ic_document_grey, "View My Reports") {
            startActivity(Intent(this, ViewReportsActivity::class.java))
        }
        
        setupAction(binding.actionSecurity, R.drawable.ic_shield_grey, "Account Security") {
             startActivity(Intent(this, SecuritySettingsActivity::class.java))
        }
        
        setupAction(binding.actionSession, R.drawable.ic_device_grey, "Session Management") {
             startActivity(Intent(this, ActiveSessionsActivity::class.java))
        }
        
        setupAction(binding.actionAccountSettings, R.drawable.ic_settings_outline_black, "Account Settings") {
             startActivity(Intent(this, AccountActionsActivity::class.java))
        }

        setupAction(binding.actionSettings, R.drawable.ic_gear_grey, "Settings") {
             startActivity(Intent(this, SettingsHomeActivity::class.java))
        }

        setupAction(binding.actionFeedback, R.drawable.ic_email_grey, "Send Feedback") {
             showFeedbackDialog()
        }

        setupAction(binding.actionLogout, R.drawable.ic_logout_grey, "Logout") {
             logoutUser()
        }
    }
    
    // --- Feedback Dialog ---
    private fun showFeedbackDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_feedback, null)
        val etFeedback = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFeedback)
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Send Feedback")
            .setView(dialogView)
            .setCancelable(false)
            .setNeutralButton("Send Manually") { _, _ ->
                val text = etFeedback.text.toString().trim()
                sendFeedbackManual(text)
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Submit", null) // Override later to prevent auto-close
            .create()
            
        dialog.show()
        
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = etFeedback.text.toString().trim()
            if (text.isEmpty()) {
                etFeedback.error = "Please enter feedback"
            } else {
                submitFeedback(text)
                dialog.dismiss()
            }
        }
    }
    
    private fun submitFeedback(content: String) {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@ProfileAccountActivity)
                val response = api.submitFeedback(mapOf("feedback" to content))
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileAccountActivity, "Feedback sent! Thank you.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileAccountActivity, "Failed to send feedback", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                 withContext(kotlinx.coroutines.Dispatchers.Main) {
                     Toast.makeText(this@ProfileAccountActivity, "Feedback queued (Offline)", Toast.LENGTH_SHORT).show()
                 }
            }
        }
    }

    private fun sendFeedbackManual(content: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("vigilantappdetection@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Vigilant App Feedback")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
             Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Logout ---
    private fun logoutUser() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // 1. Call API to invalidate session (optional best practice)
                 // val api = com.simats.vigilant.data.api.ApiClient.getService(this@ProfileAccountActivity)
                 // api.logout() // If exists
                
                // 2. Clear Local Prefs
                val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val intent = Intent(this@ProfileAccountActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Force logout anyway
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    startActivity(Intent(this@ProfileAccountActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
    
    private fun loadProfileData() {
        // 1. Load from Cache first (SharedPreferences)
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        updateUIFromPrefs(prefs)
        
        // 2. Refresh from Backend
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = applicationContext
                val api = com.simats.vigilant.data.api.ApiClient.getService(context)
                
                val response = api.getProfile()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val profileData = response.body()?.data
                    if (profileData != null) {
                        // Save to Prefs
                        with(prefs.edit()) {
                            putString("profile_name", profileData.full_name ?: "Vigilant User")
                            putString("profile_email", profileData.email ?: "user@example.com")
                            // Backend might return device count too, valuable to store?
                            putInt("device_count", profileData.device_count ?: 1)
                            apply()
                        }
                        
                        // Update UI on Main Thread
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            updateUIFromPrefs(prefs)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Keep showing cached data
            }
        }
    }
    
    private fun updateUIFromPrefs(prefs: android.content.SharedPreferences) {
        val name = prefs.getString("profile_name", "Vigilant User") ?: "Vigilant User"
        val email = prefs.getString("profile_email", "user@example.com") ?: "user@example.com"
        
        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        
        // Calculate Initials
        val initials = name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
        
        binding.tvProfileInitials.text = if (initials.isNotEmpty()) initials else "JD"
        
        val savedUri = prefs.getString("profile_image_uri", null)
        if (savedUri != null) {
             try {
                 binding.ivProfileAvatar.setImageURI(android.net.Uri.parse(savedUri))
                 binding.tvProfileInitials.visibility = android.view.View.GONE
             } catch (e: Exception) {
                 binding.tvProfileInitials.visibility = android.view.View.VISIBLE
             }
        } else {
             binding.tvProfileInitials.visibility = android.view.View.VISIBLE
             binding.ivProfileAvatar.setImageResource(R.drawable.bg_circle_avatar) 
        }
    }
    
    private fun setupAction(itemBinding: ItemProfileActionBinding, iconRes: Int, title: String, onClick: () -> Unit) {
        itemBinding.iconAction.setImageResource(iconRes)
        itemBinding.textActionTitle.text = title
        itemBinding.root.setOnClickListener { onClick() }
    }
}
