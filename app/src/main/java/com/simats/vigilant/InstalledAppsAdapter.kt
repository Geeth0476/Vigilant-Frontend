package com.simats.vigilant

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class InstalledAppsAdapter(
    private val context: Context,
    private var apps: List<InstalledAppRepository.InstalledApp>
) : RecyclerView.Adapter<InstalledAppsAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvPackageName: TextView = view.findViewById(R.id.tvPackageName)
        val tvRiskDescription: TextView = view.findViewById(R.id.tvRiskDescription)
        val ivAppIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvRiskScore: TextView = view.findViewById(R.id.tvRiskScore)
        val ivRiskIcon: ImageView = view.findViewById(R.id.ivRiskIcon)
        val llRiskScoreParams: LinearLayout = view.findViewById(R.id.llRiskScoreParams)
        val flIconContainer: FrameLayout = view.findViewById(R.id.flIconContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_installed_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        
        holder.tvAppName.text = app.appName
        holder.tvPackageName.text = app.packageName
        holder.tvRiskDescription.text = app.riskDescription
        
        // Load Icon
        try {
            val icon = context.packageManager.getApplicationIcon(app.packageName)
            holder.ivAppIcon.setImageDrawable(icon)
        } catch (e: Exception) {
            holder.ivAppIcon.setImageResource(R.drawable.ic_vigilant_logo) // Fallback
        }

        // Style based on risk score
        val riskScore = app.riskScore
        holder.tvRiskScore.text = riskScore.toString()

        if (riskScore >= 70) {
            // High Risk
            holder.llRiskScoreParams.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_red_bg_light)
            holder.flIconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_red_bg_light)
            holder.tvRiskScore.setTextColor(ContextCompat.getColor(context, R.color.vigilant_red))
            holder.ivRiskIcon.setImageResource(R.drawable.ic_shield_alert_outline_red) // Ensure this exists or use fallback
            holder.tvRiskDescription.setTextColor(ContextCompat.getColor(context, R.color.vigilant_red))
        } else if (riskScore >= 40) {
            // Medium Risk
            holder.llRiskScoreParams.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_amber_bg_light)
            holder.flIconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_amber_bg_light)
            holder.tvRiskScore.setTextColor(ContextCompat.getColor(context, R.color.vigilant_amber))
             // Assuming amber icons exist, otherwise default logic
             holder.ivRiskIcon.setImageResource(R.drawable.ic_shield_alert_outline_red) // reuse alert for now or check assets
             holder.tvRiskDescription.setTextColor(ContextCompat.getColor(context, R.color.vigilant_amber))
        } else {
            // Safe
            holder.llRiskScoreParams.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_green_bg_light)
            holder.flIconContainer.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_green_bg_light)
            holder.tvRiskScore.setTextColor(ContextCompat.getColor(context, R.color.vigilant_green))
            holder.ivRiskIcon.setImageResource(R.drawable.ic_check_circle_green)
            holder.tvRiskDescription.setTextColor(ContextCompat.getColor(context, R.color.vigilant_text_secondary))
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, AppAnalysisActivity::class.java)
            intent.putExtra("APP_NAME", app.appName)
            intent.putExtra("PACKAGE_NAME", app.packageName)
            context.startActivity(intent)
            if (context is android.app.Activity) {
                context.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }

    override fun getItemCount() = apps.size
    
    fun updateList(newApps: List<InstalledAppRepository.InstalledApp>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
