package com.simats.vigilant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.simats.vigilant.ReportRepository.ReportItem
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ViewReportsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_reports)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setNavigationOnClickListener {
            finish()
        }

        val rvReports = findViewById<RecyclerView>(R.id.rvReports)
        rvReports.layoutManager = LinearLayoutManager(this)
        
        // Use Repository
        rvReports.adapter = ReportsAdapter(ReportRepository.reports)
        
        checkEmptyState()
        
        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fabReport).setOnClickListener {
            android.content.Intent(this, ReportAppActivity::class.java).also {
                startActivity(it)
            }
        }
        
        topAppBar.menu.add(0, 1, 0, "Clear All")
        topAppBar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == 1) {
                ReportRepository.clear()
                rvReports.adapter?.notifyDataSetChanged()
                checkEmptyState()
                true
            } else {
                false
            }
        }
    }
    
    private fun checkEmptyState() {
        val emptyView = findViewById<View>(R.id.layoutEmptyState)
        val rv = findViewById<RecyclerView>(R.id.rvReports)
        
        if (ReportRepository.reports.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }

    private fun loadReports() {
         lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = applicationContext
                val api = com.simats.vigilant.data.api.ApiClient.getService(context)
                val response = api.getMyReports()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data
                    
                    if (items != null) {
                        ReportRepository.clear()
                        items.forEach { item ->
                            ReportRepository.addReport(ReportRepository.ReportItem(
                                name = item.app_name,
                                type = item.report_type,
                                date = item.created_at, // Consider formatting date
                                status = item.status
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to existing or empty
            }
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                findViewById<RecyclerView>(R.id.rvReports).adapter?.notifyDataSetChanged()
                checkEmptyState()
            }
         }
    }

    class ReportsAdapter(private val reports: List<ReportItem>) : RecyclerView.Adapter<ReportsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvAppName: TextView = view.findViewById(R.id.tvAppName)
            val tvType: TextView = view.findViewById(R.id.tvType)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_report_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = reports[position]
            holder.tvAppName.text = item.name
            holder.tvType.text = item.type
            holder.tvDate.text = item.date
            // Format status text (capitalize first letter if needed, or just use as is)
            val statusText = item.status.lowercase().replaceFirstChar { it.uppercase() }
            holder.tvStatus.text = "• $statusText"
            
            val greenStatus = listOf("Resolved", "Reviewed", "Approved")
            
            if (greenStatus.any { it.equals(statusText, ignoreCase = true) }) {
                holder.tvStatus.setTextColor(0xFF4CAF50.toInt()) // Green
            } else {
                 holder.tvStatus.setTextColor(0xFFF57C00.toInt()) // Orange
            }
            
            holder.itemView.setOnClickListener {
                val intent = android.content.Intent(holder.itemView.context, ReportDetailActivity::class.java)
                intent.putExtra("APP_NAME", item.name)
                intent.putExtra("REPORT_TYPE", item.type)
                intent.putExtra("REPORT_DATE", item.date)
                intent.putExtra("REPORT_STATUS", item.status)
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount() = reports.size
    }
}
