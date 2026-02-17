package com.simats.vigilant

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.simats.vigilant.databinding.ActivityCommunityFeedBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CommunityFeedActivity : BaseActivity() {

    private lateinit var binding: ActivityCommunityFeedBinding
    private lateinit var adapter: CommunityFeedAdapter
    private var currentCategory: String = "All"
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_info -> {
                    startActivity(Intent(this, CommunityInfoActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Setup RecyclerView
        binding.rvCommunityFeed.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        adapter = CommunityFeedAdapter(CommunityRepository.threats)
        binding.rvCommunityFeed.adapter = adapter
        
        // Setup filter chips
        setupFilterChips()
        setupSearch()

        binding.fabReport.setOnClickListener {
             startActivity(Intent(this, ReportAppActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
             fetchCommunityThreats()
        }
        
        setupBottomNav()
        
        // Initial Fetch
        fetchCommunityThreats()
    }
    
    private fun fetchCommunityThreats() {
        lifecycleScope.launch {
            try {
                binding.swipeRefresh.isRefreshing = true
                val api = com.simats.vigilant.data.api.ApiClient.getService(this@CommunityFeedActivity)
                val response = api.getCommunityThreats()
                if (response.isSuccessful && response.body()?.success == true) {
                    val apiThreats = response.body()?.data ?: emptyList()
                    
                    // Map to UI Model
                    val uiThreats = apiThreats.map { apiT ->
                        CommunityThreat(
                            id = apiT.id.toString(),
                            appName = apiT.app_name,
                            packageName = apiT.package_name,
                            category = apiT.category,
                            riskLevel = apiT.risk_level,
                            reportCount = apiT.report_count,
                            firstSeen = apiT.first_seen_at ?: "Unknown",
                            lastReported = apiT.last_reported_at ?: "Recently",
                            behaviors = if (!apiT.behaviors.isNullOrEmpty()) listOf(apiT.behaviors) else emptyList(),
                            description = apiT.description
                        )
                    }
                    
                    // Update Repository so Detail Activity can find them
                    CommunityRepository.updateThreats(uiThreats)
                    
                    allLoadedThreats = uiThreats
                    applyFilters()
                    
                } else {
                    // Fail silently or show error
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private var allLoadedThreats: List<CommunityThreat> = CommunityRepository.threats
    
    private fun setupBottomNav() {
        setupBottomNavigation(binding.bottomNavigation, R.id.nav_community)
    }
    
    private fun setupFilterChips() {
        val allChips: List<com.google.android.material.chip.Chip> = listOf(binding.chipAll, binding.chipSpyware, binding.chipStalkerware, binding.chipHighRisk)
        
        // Initial state
        updateChipVisuals(binding.chipAll, allChips)
        
        binding.chipAll.setOnClickListener {
            currentCategory = "All"
            updateChipVisuals(binding.chipAll, allChips)
            applyFilters()
        }
        
        binding.chipSpyware.setOnClickListener {
            currentCategory = "Spyware"
            updateChipVisuals(binding.chipSpyware, allChips)
            applyFilters()
        }
        
        binding.chipStalkerware.setOnClickListener {
            currentCategory = "Stalkerware"
            updateChipVisuals(binding.chipStalkerware, allChips)
            applyFilters()
        }
        
        binding.chipHighRisk.setOnClickListener {
            currentCategory = "High Risk" // Special logic for risk level?
            updateChipVisuals(binding.chipHighRisk, allChips)
            applyFilters()
        }
    }
    
    private fun updateChipVisuals(selected: com.google.android.material.chip.Chip, all: List<com.google.android.material.chip.Chip>) {
        all.forEach { chip ->
            if (chip == selected) {
                chip.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(this, R.color.vigilant_blue)
                chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.white))
                chip.chipStrokeWidth = 0f
                chip.isChecked = true
            } else {
                chip.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(this, R.color.white)
                chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.black))
                chip.chipStrokeColor = androidx.core.content.ContextCompat.getColorStateList(this, R.color.vigilant_text_secondary)
                chip.chipStrokeWidth = 2f
                chip.isChecked = false
            }
        }
    }
    
    private fun setupSearch() {
        binding.etSearchCommunity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun applyFilters() {
        val filtered = allLoadedThreats.filter { threat ->
            val matchesCategory = when(currentCategory) {
                "All" -> true
                "High Risk" -> threat.riskLevel == "Critical" || threat.riskLevel == "High"
                else -> threat.category.equals(currentCategory, ignoreCase = true)
            }
            
            val matchesSearch = threat.appName.contains(currentQuery, ignoreCase = true) || 
                                threat.description.contains(currentQuery, ignoreCase = true)
                                
            matchesCategory && matchesSearch
        }
        
        if (filtered.isEmpty()) {
            binding.rvCommunityFeed.visibility = android.view.View.GONE
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
        } else {
            binding.rvCommunityFeed.visibility = android.view.View.VISIBLE
            binding.layoutEmptyState.visibility = android.view.View.GONE
            adapter.updateList(filtered)
            binding.rvCommunityFeed.scheduleLayoutAnimation()
        }
    }
    
    class CommunityFeedAdapter(private var items: List<CommunityThreat>) : 
        androidx.recyclerview.widget.RecyclerView.Adapter<CommunityFeedAdapter.ViewHolder>() {
        
        fun updateList(newItems: List<CommunityThreat>) {
            items = newItems
            notifyDataSetChanged()
        }

        class ViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvAppName: android.widget.TextView = view.findViewById(R.id.tvAppName)
            val tvRiskBadge: android.widget.TextView = view.findViewById(R.id.tvRiskBadge)
            val llRiskBadge: android.view.View = view.findViewById(R.id.llRiskBadge)
            val tvDescription: android.widget.TextView = view.findViewById(R.id.tvDescription)
            val tvFooterStats: android.widget.TextView = view.findViewById(R.id.tvFooterStats)
            val flIconContainer: android.view.View = view.findViewById(R.id.flIconContainer)
            val ivAppIcon: android.widget.ImageView = view.findViewById(R.id.ivAppIcon)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_community_threat, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvAppName.text = item.appName
            holder.tvDescription.text = item.description
            holder.tvFooterStats.text = "Reported by ${item.reportCount} users • ${item.lastReported}"
            holder.tvRiskBadge.text = item.riskLevel
            
            // Branding/Colors based on Risk
            val context = holder.itemView.context
            val (colorRes, bgHex, iconRes) = when(item.riskLevel.uppercase()) {
                "CRITICAL" -> Triple(R.color.vigilant_red, "#FFEBEE", R.drawable.ic_alert_red)
                "HIGH" -> Triple(R.color.vigilant_red, "#FFEBEE", R.drawable.ic_warning_amber) // Should use red
                "MEDIUM" -> Triple(R.color.vigilant_amber, "#FFF8E1", R.drawable.ic_warning_amber)
                else -> Triple(R.color.vigilant_green, "#E8F5E9", R.drawable.ic_check_circle_green)
            }
            
            val color = androidx.core.content.ContextCompat.getColor(context, colorRes)
            holder.tvRiskBadge.setTextColor(color)
            holder.llRiskBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(bgHex))
            
            holder.flIconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(bgHex))
            holder.ivAppIcon.setImageResource(iconRes)
            holder.ivAppIcon.setColorFilter(if (item.riskLevel.uppercase() == "LOW") color else 0) // Tint green check if safe, else stick to icon color (usually embedded)
            // Actually my icons like ic_mic_red are red. I should probably trust the icon. 
            // Better behavior: clear color filter if reusing view
            if (item.riskLevel.uppercase() != "LOW") {
                 holder.ivAppIcon.clearColorFilter()
            }
            
            holder.itemView.setOnClickListener {
                val intent = Intent(context, CommunityThreatDetailActivity::class.java)
                intent.putExtra("THREAT_ID", item.id)
                intent.putExtra("APP_NAME", item.appName) // Fallback
                context.startActivity(intent)
                if (context is android.app.Activity) {
                    context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
