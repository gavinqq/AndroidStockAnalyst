package com.autostockplan.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.autostockplan.app.R
import com.autostockplan.app.data.ExecutionPlan
import java.text.SimpleDateFormat
import java.util.*

class ExecutionPlanAdapter(
    private val plans: List<ExecutionPlan>,
    private val onItemClick: (ExecutionPlan) -> Unit
) : RecyclerView.Adapter<ExecutionPlanAdapter.PlanViewHolder>() {

    class PlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val planId: TextView = itemView.findViewById(R.id.tvPlanId)
        val planPreview: TextView = itemView.findViewById(R.id.tvPlanPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_execution_plan, parent, false)
        return PlanViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = plans[position]
        
        // Format the ID as a readable datetime
        val formattedDate = try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val date = sdf.parse(plan.id)
            val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            displayFormat.format(date ?: Date())
        } catch (e: Exception) {
            plan.id
        }
        
        holder.planId.text = formattedDate
        holder.planPreview.text = plan.content.take(150) + if (plan.content.length > 150) "..." else ""
        
        holder.itemView.setOnClickListener {
            onItemClick(plan)
        }
    }

    override fun getItemCount(): Int = plans.size
}
