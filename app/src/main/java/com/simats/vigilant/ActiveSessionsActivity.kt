package com.simats.vigilant

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActiveSessionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_sessions)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        findViewById<MaterialButton>(R.id.btnSignOutAll).setOnClickListener {
             androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign Out All Devices?")
                .setMessage("This will sign you out from all other devices except this one. Are you sure?")
                .setPositiveButton("Sign Out All") { _, _ ->
                     lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                         try {
                             val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                             val response = api.revokeAllSessions()
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                 if (response.isSuccessful && response.body()?.success == true) {
                                     Toast.makeText(applicationContext, "All other sessions terminated", Toast.LENGTH_SHORT).show()
                                     loadSessions()
                                 } else {
                                     Toast.makeText(applicationContext, "Failed to sign out all", Toast.LENGTH_SHORT).show()
                                 }
                             }
                         } catch (e: Exception) {
                             e.printStackTrace()
                             withContext(kotlinx.coroutines.Dispatchers.Main) {
                                Toast.makeText(applicationContext, "Network error", Toast.LENGTH_SHORT).show()
                             }
                         }
                     }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        loadSessions()
    }
    
    private fun loadSessions() {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = applicationContext
                val deviceId = com.simats.vigilant.data.DeviceIdManager.getDeviceId(context)
                val api = com.simats.vigilant.data.api.ApiClient.getService(context)
                
                val response = api.getDevices(deviceId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val devices = response.body()?.data
                    if (devices != null) {
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            updateUI(devices, deviceId) // or match by ID if API returns it
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun updateUI(devices: List<com.simats.vigilant.data.model.DeviceItem>, currentDeviceId: String) {
        // Find current device - simplified matching by ID/Name/Flag
        // Backend 'getDevices' might flag 'is_current'
        val currentDevice = devices.find { it.is_current == true } ?: devices.firstOrNull()
        
        if (currentDevice != null) {
            findViewById<android.widget.TextView>(R.id.tvCurrentDeviceTitle)?.text = currentDevice.device_name
            findViewById<android.widget.TextView>(R.id.tvCurrentDeviceDetail)?.text = "${currentDevice.manufacturer ?: "Android"} • ${currentDevice.last_active ?: "Active Now"}"
        }
        
        val container = findViewById<android.widget.LinearLayout>(R.id.llSessionContainer)
        container.removeAllViews()
        
        devices.filter { it.is_current != true }.forEach { device ->
            val itemView = layoutInflater.inflate(R.layout.item_profile_action, container, false)
            
            // Customize item view directly assuming item_profile_action structure
            val icon = itemView.findViewById<android.widget.ImageView>(R.id.iconAction)
            if (device.manufacturer?.contains("Window", true) == true) {
                 icon.setImageResource(R.drawable.ic_device_grey) // computer icon
            } else {
                 icon.setImageResource(R.drawable.ic_phone_android_blue)
            }
            
            val title = itemView.findViewById<android.widget.TextView>(R.id.textActionTitle)
            title.text = "${device.device_name}\n${device.manufacturer ?: "Unknown"} • ${device.last_active ?: "Recently"}"
            
            itemView.setOnClickListener {
                showTerminationDialog(device.device_name, device.id.toString()) {
                    // Refresh after revoke
                    loadSessions()
                }
            }
            
            container.addView(itemView)
            
            // Add spacer
            val spacer = android.view.View(this)
            spacer.layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                (16 * resources.displayMetrics.density).toInt()
            )
            container.addView(spacer)
        }
    }

    private fun showTerminationDialog(sessionName: String, deviceId: String, onRevoke: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Terminate Session?")
            .setMessage("Do you want to sign out of $sessionName? This will revoke access immediately.")
            .setPositiveButton("Terminate") { _, _ ->
                 lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                     try {
                         val api = com.simats.vigilant.data.api.ApiClient.getService(applicationContext)
                         api.revokeDevice(deviceId)
                         withContext(kotlinx.coroutines.Dispatchers.Main) {
                             Toast.makeText(applicationContext, "Session terminated.", Toast.LENGTH_SHORT).show()
                             onRevoke()
                         }
                     } catch (e: Exception) { e.printStackTrace() }
                 }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
