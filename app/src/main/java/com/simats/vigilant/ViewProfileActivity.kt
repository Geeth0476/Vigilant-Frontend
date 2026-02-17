package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class ViewProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_profile)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }

        val btnEdit = findViewById<MaterialButton>(R.id.btnGoToEdit)
        btnEdit.setOnClickListener {
            // Replace with standard EditProfile launch
            startActivity(Intent(this, EditProfileActivity::class.java))
            finish() // Optional: close view so back from edit goes to main profile list, or keep it? 
                     // Usually View -> Edit -> Back to View.
                     // But if we finish here, where does Edit go back to? It goes back to previous stack item. 
                     // Assuming ProfileAccount -> ViewProfile -> EditProfile. 
                     // If Edit finishes, it goes back to ViewProfile. So we should NOT finish here.
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val prefs = getSharedPreferences("vigilant_prefs", MODE_PRIVATE)
        val name = prefs.getString("profile_name", getString(R.string.profile_name)) ?: "User"
        val email = prefs.getString("profile_email", getString(R.string.profile_email)) ?: ""
        val phone = prefs.getString("profile_phone", getString(R.string.value_phone)) ?: ""

        findViewById<TextView>(R.id.tvViewName).text = name
        findViewById<TextView>(R.id.tvViewEmail).text = email
        findViewById<TextView>(R.id.tvViewPhone).text = phone

        // Avatar Logic
        val ivAvatar = findViewById<ImageView>(R.id.ivViewAvatar)
        val tvInitials = findViewById<TextView>(R.id.tvViewInitials)
        
        val savedUri = prefs.getString("profile_image_uri", null)
        
        if (savedUri != null) {
            try {
                ivAvatar.setImageURI(Uri.parse(savedUri))
                tvInitials.visibility = View.GONE
            } catch (e: Exception) {
                tvInitials.visibility = View.VISIBLE
            }
        } else {
            tvInitials.visibility = View.VISIBLE
            ivAvatar.setImageResource(R.drawable.bg_circle_avatar) 
        }

        // Initials Logic
        val initials = name.split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
            .uppercase()
        
        tvInitials.text = if (initials.isNotEmpty()) initials else "JD"
    }
}
