package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.vigilant.databinding.ActivityInstalledAppsBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsActivity : BaseActivity() {

    private lateinit var binding: ActivityInstalledAppsBinding
    private lateinit var adapter: InstalledAppsAdapter
    private var allAppsList: List<InstalledAppRepository.InstalledApp> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstalledAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list when returning (e.g. from uninstall)
        loadAppsAsync()
    }

    private fun setupUI() {
        // Setup top app bar menu
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsHomeActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        setupBottomNavigation(binding.bottomNavigation, R.id.nav_apps)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup filter chips
        setupFilterChips()
        
        // Setup search
        setupSearch()
    }
    
    private fun setupRecyclerView() {
        binding.rvInstalledApps.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        if (InstalledAppRepository.installedApps.isNotEmpty()) {
             allAppsList = InstalledAppRepository.installedApps.toList()
             adapter = InstalledAppsAdapter(this, allAppsList)
             binding.rvInstalledApps.adapter = adapter
             binding.rvInstalledApps.scheduleLayoutAnimation()
             checkEmptyState(allAppsList.isEmpty())
             
             handleDeepLink()
        } else {
             adapter = InstalledAppsAdapter(this, emptyList())
             binding.rvInstalledApps.adapter = adapter
        }
        
        // Always refresh in background to catch new installs
        loadAppsAsync()
    }

    private fun handleDeepLink() {
        val targetPackage = intent.getStringExtra("PACKAGE_NAME")
        if (targetPackage != null) {
             val appTitle = allAppsList.find { it.packageName == targetPackage }?.appName
             if (appTitle != null) {
                 filterApps(appTitle)
                 binding.etSearchApps.setText(appTitle)
             }
        }
    }

    private fun loadAppsAsync() {
        binding.rvInstalledApps.visibility = android.view.View.GONE
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Use local calculation exclusively for real-time accuracy
            InstalledAppRepository.populate(this@InstalledAppsActivity)
            
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                binding.progressBar.visibility = android.view.View.GONE
                allAppsList = InstalledAppRepository.installedApps.toList()
                adapter = InstalledAppsAdapter(this@InstalledAppsActivity, allAppsList)
                binding.rvInstalledApps.adapter = adapter
                binding.rvInstalledApps.scheduleLayoutAnimation()
                checkEmptyState(allAppsList.isEmpty())
                handleDeepLink()
            }
        }
    }

    private fun filterApps(query: String) {
        val q = query.lowercase()
        val filtered = allAppsList.filter { 
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
        adapter.updateList(filtered)
        checkEmptyState(filtered.isEmpty())
    }
    
    private fun filterByRisk(level: String) {
        val filtered = when(level) {
            "high" -> allAppsList.filter { it.riskScore >= 70 }
            "medium" -> allAppsList.filter { it.riskScore >= 40 && it.riskScore < 70 }
            "safe" -> allAppsList.filter { it.riskScore < 40 }
            else -> allAppsList
        }
        adapter.updateList(filtered)
        checkEmptyState(filtered.isEmpty())
    }

    private fun checkEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutEmptyState.root.visibility = android.view.View.VISIBLE
            binding.rvInstalledApps.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.root.visibility = android.view.View.GONE
            binding.rvInstalledApps.visibility = android.view.View.VISIBLE
        }
    }
    
    // Add setupFilterChips implementation
    private fun setupFilterChips() {
        // Set initial state
        updateChipSelection(binding.chipAllApps, binding.chipHighRiskApps, binding.chipMediumRiskApps, binding.chipSafeApps)
        
        binding.chipAllApps.setOnClickListener {
            updateChipSelection(binding.chipAllApps, binding.chipHighRiskApps, binding.chipMediumRiskApps, binding.chipSafeApps)
            filterByRisk("all")
        }
        
        binding.chipHighRiskApps.setOnClickListener {
            updateChipSelection(binding.chipHighRiskApps, binding.chipAllApps, binding.chipMediumRiskApps, binding.chipSafeApps)
            filterByRisk("high")
        }
        
        binding.chipMediumRiskApps.setOnClickListener {
            updateChipSelection(binding.chipMediumRiskApps, binding.chipAllApps, binding.chipHighRiskApps, binding.chipSafeApps)
            filterByRisk("medium")
        }
        
        binding.chipSafeApps.setOnClickListener {
            updateChipSelection(binding.chipSafeApps, binding.chipAllApps, binding.chipHighRiskApps, binding.chipMediumRiskApps)
            filterByRisk("safe")
        }
    }

    // Add setupSearch implementation
    private fun setupSearch() {
        binding.etSearchApps.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    
    private fun updateChipSelection(
        selected: com.google.android.material.chip.Chip,
        vararg others: com.google.android.material.chip.Chip
    ) {
        // Set selected chip to blue background with white text, no stroke
        selected.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(this, R.color.vigilant_blue)
        selected.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
        selected.chipStrokeWidth = 0f
        selected.isChecked = true
        
        // Set other chips to white background with black text and stroke
        others.forEach { chip ->
            chip.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(this, R.color.white)
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.black))
            chip.chipStrokeColor = androidx.core.content.ContextCompat.getColorStateList(this, R.color.vigilant_text_secondary)
            chip.chipStrokeWidth = 2f
            chip.isChecked = false
        }
    }
}
