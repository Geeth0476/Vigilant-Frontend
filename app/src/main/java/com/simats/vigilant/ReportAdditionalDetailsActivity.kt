package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.simats.vigilant.ReportRepository.ReportItem
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportAdditionalDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_additional_details)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        findViewById<MaterialButton>(R.id.btnSubmitReport).setOnClickListener {
             val appName = intent.getStringExtra("APP_NAME") ?: "Unknown App"
             val type = intent.getStringExtra("REPORT_TYPE") ?: "OTHER"
             val desc = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAdditionalDetails).text.toString()
             
             // Map display category to Backend ENUM
             val categoryEnum = when(type) {
                 "Data Theft" -> "DATA_THEFT"
                 "Permission Abuse" -> "PERMISSION_ABUSE"
                 "Stalkerware" -> "STALKERWARE"
                 "Spyware" -> "SPYWARE"
                 else -> "OTHER"
             }
             
             // Prepare data
             val reportData = mapOf(
                 "app_name" to appName,
                 "package_name" to "com.example.unknown", // Ideal: get actual package. MVP: fake or pass from previous screen
                 "category" to categoryEnum,
                 "description" to (if (desc.isNotBlank()) desc else "User reported suspicious behavior"),
                 "additional_details" to desc
             )

             // Call API
             lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                 try {
                     val context = applicationContext
                     // Try to resolve package name from installed apps if possible
                     val pkgName = com.simats.vigilant.InstalledAppRepository.installedApps.find { it.appName == appName }?.packageName ?: "com.unknown.app"
                     
                     val finalData = reportData.toMutableMap()
                     finalData["package_name"] = pkgName
                     
                     val api = com.simats.vigilant.data.api.ApiClient.getService(context)
                     val response = api.submitReport(finalData)
                     
                     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                         if (response.isSuccessful && response.body()?.success == true) {
                             // Add to local repo for immediate feedback (optional, since ViewReports refreshes)
                             val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date())
                             ReportRepository.addReport(ReportItem(appName, type, date, "Under Review"))
                             
                             startActivity(Intent(this@ReportAdditionalDetailsActivity, ReportSuccessActivity::class.java))
                             finish()
                         } else {
                             Toast.makeText(context, "Failed to submit: ${response.message()}", Toast.LENGTH_SHORT).show()
                         }
                     }
                 } catch (e: Exception) {
                     e.printStackTrace()
                     kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                         Toast.makeText(applicationContext, "Network Error", Toast.LENGTH_SHORT).show()
                     }
                 }
             }
        }
    }
}
