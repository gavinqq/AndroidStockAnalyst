package com.autostockplan.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.autostockplan.app.R
import com.autostockplan.app.utils.ExecutionPlanManager
import java.text.SimpleDateFormat
import java.util.*

class ExecutionPlanDetailActivity : AppCompatActivity() {
    
    private lateinit var planManager: ExecutionPlanManager
    private lateinit var tvPlanContent: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_execution_plan_detail)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        planManager = ExecutionPlanManager(this)
        tvPlanContent = findViewById(R.id.tvPlanContent)
        
        val planId = intent.getStringExtra("plan_id") ?: ""
        
        if (planId.isNotEmpty()) {
            val plan = planManager.loadPlan("$planId.txt")
            if (plan != null) {
                // Format the ID as a readable datetime for title
                val formattedDate = try {
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val date = sdf.parse(plan.id)
                    val displayFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    displayFormat.format(date ?: Date())
                } catch (e: Exception) {
                    plan.id
                }
                
                supportActionBar?.title = formattedDate
                tvPlanContent.text = plan.content
            } else {
                tvPlanContent.text = "Plan not found"
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
