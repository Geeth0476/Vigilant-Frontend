package com.simats.vigilant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        // Get SharedPreferences
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        
        // Load current values
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etCountryCode = findViewById<TextInputEditText>(R.id.etCountryCode)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val tvInitials = findViewById<android.widget.TextView>(R.id.tvAvatarInitials)
        
        fun updateInitials(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                tvInitials.text = ""
                return
            }
            val parts = trimmed.split("\\s+".toRegex())
            val initials = when {
                parts.size >= 2 -> "${parts[0].first()}${parts.last().first()}"
                parts.size == 1 && trimmed.length >= 2 -> trimmed.take(2)
                parts.size == 1 -> trimmed
                else -> ""
            }.uppercase()
            tvInitials.text = initials
        }
        
        etFullName.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updateInitials(s.toString())
            }
        })
        
        etFullName.setText(prefs.getString("profile_name", getString(R.string.profile_name)))
        updateInitials(etFullName.text.toString())
        etEmail.setText(prefs.getString("profile_email", getString(R.string.profile_email)))
        
        // Parse Phone
        val fullPhone = prefs.getString("profile_phone", null)
        if (fullPhone != null) {
            val parts = fullPhone.trim().split(" ", limit = 2)
            if (parts.size > 1) {
                etCountryCode.setText(parts[0])
                etPhone.setText(parts[1])
            } else {
                // Formatting issue or just one part
                if (fullPhone.startsWith("+")) {
                     etCountryCode.setText(fullPhone)
                     etPhone.setText("")
                } else {
                     etCountryCode.setText("+91")
                     etPhone.setText(fullPhone)
                }
            }
        } else {
            etCountryCode.setText("+91")
            etPhone.setText("")
        }
        
        // Image Picker Logic
        val ivAvatar = findViewById<android.widget.ImageView>(R.id.ivAvatar)
        // tvInitials is already defined above
        
        // Load existing image if any
        val savedUri = prefs.getString("profile_image_uri", null)
        if (savedUri != null) {
            ivAvatar.setImageURI(android.net.Uri.parse(savedUri))
            tvInitials.visibility = android.view.View.GONE
        }
        
        val pickMedia = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                // Persist the image by copying to internal storage
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val fileName = "profile_image.jpg"
                    val file = java.io.File(filesDir, fileName)
                    val outputStream = java.io.FileOutputStream(file)
                    
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val localUri = android.net.Uri.fromFile(file)
                    ivAvatar.setImageURI(localUri)
                    tvInitials.visibility = android.view.View.GONE
                    
                    // Save the LOCAL uri immediately or stage it? 
                    // Better to stage it, but simplest is saving to prefs on "Save" logic.
                    // However, we need to pass this new URI to the save logic.
                    // We'll store it in a temp variable or just overwrite prefs on Save
                    // actually, let's just update the view here and save the URI string on Save.
                    // But we need the URI string available then.
                    ivAvatar.tag = localUri.toString() 
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        ivAvatar.setOnClickListener {
            pickMedia.launch("image/*")
        }
        
        findViewById<android.view.View>(R.id.btnEditAvatar).setOnClickListener {
            pickMedia.launch("image/*")
        }
        
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            finish()
        }
        
        findViewById<MaterialButton>(R.id.btnSaveChanges).setOnClickListener {
            val newName = etFullName.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()
            val code = etCountryCode.text.toString().trim()
            val number = etPhone.text.toString().trim()
            val newPhone = "$code $number"
            
            // Validate inputs
            if (newName.isEmpty()) {
                etFullName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            
            if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etEmail.error = "Please enter a valid email"
                return@setOnClickListener
            }
            
            // Show loading state (block double clicks)
            val btn = findViewById<MaterialButton>(R.id.btnSaveChanges)
            btn.isEnabled = false
            btn.text = "Saving..."
            
            // Perform API Call
            lifecycleScope.launch {
                try {
                    val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                    
                    // Convert image to Base64 if a new one was selected
                    var profileImageBase64: String? = null
                    val newImageUriString = ivAvatar.tag as? String
                    if (newImageUriString != null) {
                        try {
                            val uri = android.net.Uri.parse(newImageUriString)
                            val inputStream = contentResolver.openInputStream(uri)
                            val bytes = inputStream?.readBytes()
                            inputStream?.close()
                            if (bytes != null) {
                                profileImageBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val request = com.simats.vigilant.data.model.UpdateProfileRequest(
                        full_name = newName,
                        email = newEmail,
                        phone = newPhone,
                        profile_image = profileImageBase64,
                        current_password = null,
                        new_password = null
                    )
                    
                    val response = api.updateProfile(request)
                    if (response.isSuccessful) {
                        // Check for new image
                        val newImageUri = ivAvatar.tag as? String
                        
                        // Save to SharedPreferences
                        prefs.edit().apply {
                            putString("profile_name", newName)
                            putString("profile_email", newEmail)
                            putString("profile_phone", newPhone)
                            if (newImageUri != null) {
                                putString("profile_image_uri", newImageUri)
                            }
                            apply()
                        }
                        
                        // Return result to ProfileAccountActivity
                        val resultIntent = Intent().apply {
                            putExtra("profile_name", newName)
                            putExtra("profile_email", newEmail)
                            putExtra("profile_phone", newPhone)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        
                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Failed to update profile: ${response.message()}", Toast.LENGTH_SHORT).show()
                        btn.isEnabled = true
                        btn.text = "Save Changes"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@EditProfileActivity, "Network error", Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                    btn.text = "Save Changes"
                }
            }
        }
    }
}
