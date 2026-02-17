package com.simats.vigilant

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class ReportAppActivity : AppCompatActivity() {

    private var selectedCategory: String = "Spyware"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_app)
        
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }
        
        // Setup dropdown
        val installedApps = InstalledAppRepository.installedApps.map { it.appName }
        val items = if (installedApps.isNotEmpty()) installedApps else listOf("Voice Recorder", "Flashlight Pro", "Calculator+", "Weather Now", "Settings")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        (findViewById<AutoCompleteTextView>(R.id.autoCompleteApp)).setAdapter(adapter)
        
        // Setup category chips
        setupCategoryChips()
        
        findViewById<MaterialButton>(R.id.btnContinue).setOnClickListener {
            val appName = findViewById<AutoCompleteTextView>(R.id.autoCompleteApp).text.toString()
            if (appName.isBlank()) {
                Toast.makeText(this, "Please select an app", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val intent = android.content.Intent(this, ReportConsentActivity::class.java)
            intent.putExtra("APP_NAME", appName)
            intent.putExtra("REPORT_TYPE", selectedCategory)
            startActivity(intent)
        }
    }
    
    private fun setupCategoryChips() {
        val chipSpyware = findViewById<Chip>(R.id.chipSpyware)
        val chipStalkerware = findViewById<Chip>(R.id.chipStalkerware)
        val chipPermAbuse = findViewById<Chip>(R.id.chipPermAbuse)
        val chipDataTheft = findViewById<Chip>(R.id.chipDataTheft)
        
        val allChips = listOf(chipSpyware, chipStalkerware, chipPermAbuse, chipDataTheft)
        
        // Set initial state
        updateChipSelection(chipSpyware, allChips)
        // Ensure initial category is set (Spyware is default in chips, but make sure text matches)
        selectedCategory = chipSpyware.text.toString()
        
        allChips.forEach { chip ->
            chip.setOnClickListener {
                updateChipSelection(chip, allChips)
                selectedCategory = chip.text.toString()
            }
        }
    }
    
    private fun updateChipSelection(selectedChip: Chip, allChips: List<Chip>) {
        allChips.forEach { chip ->
            if (chip == selectedChip) {
                // Selected chip: Red background, white text, no stroke
                chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.vigilant_red)
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                chip.chipStrokeWidth = 0f
                chip.isChecked = true
            } else {
                // Unselected chips: White background, black text, gray stroke
                chip.chipBackgroundColor = ContextCompat.getColorStateList(this, R.color.white)
                chip.setTextColor(ContextCompat.getColor(this, R.color.black))
                chip.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.vigilant_text_secondary)
                chip.chipStrokeWidth = 2f
                chip.isChecked = false
            }
        }
    }
}
