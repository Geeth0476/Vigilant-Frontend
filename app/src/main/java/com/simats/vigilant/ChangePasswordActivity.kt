package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ChangePasswordActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
             finish()
        }
    }

    private fun setupListeners() {
        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdatePassword)
        val etOld = findViewById<TextInputEditText>(R.id.etOldPassword)
        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirm = findViewById<TextInputEditText>(R.id.etConfirmPassword)

        btnUpdate.setOnClickListener {
            val oldPass = etOld.text.toString()
            val newPass = etNew.text.toString()
            val confirmPass = etConfirm.text.toString()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                showError("Please fill all fields")
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                showError("New passwords do not match")
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                showError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            updatePassword(oldPass, newPass)
        }
    }

    private fun updatePassword(old: String, new: String) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@ChangePasswordActivity)
                val response = api.changePassword(mapOf("old_password" to old, "new_password" to new))

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ChangePasswordActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to update password"
                    // Parse if JSON
                    showError("Update failed. Check credentials.")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        findViewById<MaterialButton>(R.id.btnUpdatePassword).isEnabled = !isLoading
        findViewById<MaterialButton>(R.id.btnUpdatePassword).text = if (isLoading) "Updating..." else "Update Password"
    }
}
