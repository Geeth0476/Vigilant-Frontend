package com.simats.vigilant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class RiskFactorAdapter(
    private val riskFactors: List<RiskScoreManager.RiskFactor>
) : RecyclerView.Adapter<RiskFactorAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvBehaviorTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvBehaviorDesc)
        val tvPoints: TextView = view.findViewById(R.id.tvBehaviorPoints)
        val imgIcon: ImageView = view.findViewById(R.id.imgBehaviorIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_behavior_card_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val factor = riskFactors[position]
        
        holder.tvTitle.text = factor.description
        holder.tvPoints.text = "+${factor.score} Risk Points"
        
        // Dynamic wording based on type
        when (factor.type) {
            RiskScoreManager.FactorType.PERMISSION -> {
                holder.tvDesc.text = "This permission grants access to sensitive data."
                holder.imgIcon.setImageResource(R.drawable.ic_shield_alert_outline_red) 
                // Color tinting if needed
            }
            RiskScoreManager.FactorType.BEHAVIOR -> {
                holder.tvDesc.text = "This app behavior is considered suspicious."
                holder.imgIcon.setImageResource(R.drawable.ic_alert_red) // Suspicious behavior
            }
            RiskScoreManager.FactorType.RUNTIME -> {
                holder.tvDesc.text = "Unusual activity detected in background."
                holder.imgIcon.setImageResource(R.drawable.ic_clock_outline)
            }
            RiskScoreManager.FactorType.MODIFIER -> {
                 if (factor.score > 0) {
                     holder.tvDesc.text = "This characteristic increases risk."
                     holder.imgIcon.setImageResource(R.drawable.ic_info_outline)
                 } else {
                     holder.tvDesc.text = "This characteristic reduces risk."
                     holder.tvPoints.text = "${factor.score} Risk Points" // show negative
                     holder.imgIcon.setImageResource(R.drawable.ic_check_circle_green)
                     // Could style green
                 }
            }
        }
    }

    override fun getItemCount() = riskFactors.size
}
