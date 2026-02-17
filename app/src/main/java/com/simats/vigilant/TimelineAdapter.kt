package com.simats.vigilant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TimelineAdapter(
    private val events: List<TimelineEvent>
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

    data class TimelineEvent(
        val title: String,
        val description: String,
        val time: String,
        val iconRes: Int,
        val badgeText: String,
        val isHighRisk: Boolean
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvEventDesc)
        val tvTime: TextView = view.findViewById(R.id.tvEventTime)
        val ivIcon: ImageView = view.findViewById(R.id.ivEventIcon)
        val tvBadge: TextView = view.findViewById(R.id.tvEventBadge)
        val llBadge: View = view.findViewById(R.id.llEventBadge)
        val vDot: View = view.findViewById(R.id.vTimelineDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline_event_generic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        
        holder.tvTitle.text = event.title
        holder.tvDesc.text = event.description
        holder.tvTime.text = event.time
        holder.tvBadge.text = event.badgeText
        holder.ivIcon.setImageResource(event.iconRes)
        
        val context = holder.itemView.context
        
        if (event.isHighRisk) {
            holder.vDot.setBackgroundResource(R.drawable.bg_circle_red)
            holder.llBadge.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_red_bg_light)
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.vigilant_red))
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.vigilant_red))
        } else {
            holder.vDot.setBackgroundResource(R.drawable.bg_circle_amber)
            holder.llBadge.backgroundTintList = ContextCompat.getColorStateList(context, R.color.vigilant_amber_bg_light)
            holder.tvBadge.setTextColor(ContextCompat.getColor(context, R.color.vigilant_amber))
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.vigilant_amber))
        }
    }

    override fun getItemCount() = events.size
}
