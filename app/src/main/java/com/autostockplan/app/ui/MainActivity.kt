package com.autostockplan.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.autostockplan.app.R
import com.autostockplan.app.api.ChatGPTService
import com.autostockplan.app.data.ExecutionPlan
import com.autostockplan.app.service.FolderMonitorService
import com.autostockplan.app.utils.ExecutionPlanManager
import com.autostockplan.app.utils.PreferencesManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var planManager: ExecutionPlanManager
    private lateinit var tvLatestPlan: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: ExecutionPlanAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        
        prefsManager = PreferencesManager(this)
        planManager = ExecutionPlanManager(this)
        
        tvLatestPlan = findViewById(R.id.tvLatestPlan)
        rvHistory = findViewById(R.id.rvHistory)
        
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            refreshLatestPlan()
        }
        
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            refreshLatestPlan()
        }
        
        rvHistory.layoutManager = LinearLayoutManager(this)
        
        // Start the monitoring service
        startForegroundService(Intent(this, FolderMonitorService::class.java))
        
        loadPlans()
    }
    
    override fun onResume() {
        super.onResume()
        loadPlans()
    }
    
    private fun loadPlans() {
        val latestPlan = planManager.getLatestPlan()
        if (latestPlan != null) {
            displayLatestPlan(latestPlan)
        } else {
            tvLatestPlan.text = getString(R.string.no_plan_available)
        }
        
        val allPlanIds = planManager.getAllPlanIds()
        val plans = allPlanIds.mapNotNull { planManager.loadPlan("$it.txt") }
        
        adapter = ExecutionPlanAdapter(plans) { plan ->
            val intent = Intent(this, ExecutionPlanDetailActivity::class.java).apply {
                putExtra("plan_id", plan.id)
            }
            startActivity(intent)
        }
        
        rvHistory.adapter = adapter
    }
    
    private fun displayLatestPlan(plan: ExecutionPlan) {
        val preview = plan.content.take(300) + if (plan.content.length > 300) "..." else ""
        tvLatestPlan.text = preview
        tvLatestPlan.setOnClickListener {
            val intent = Intent(this, ExecutionPlanDetailActivity::class.java).apply {
                putExtra("plan_id", plan.id)
            }
            startActivity(intent)
        }
    }
    
    private fun refreshLatestPlan() {
        val lastImagePath = prefsManager.lastImagePath
        if (lastImagePath.isEmpty()) {
            tvLatestPlan.text = "No image to analyze"
            return
        }
        
        val apiKey = prefsManager.chatGptApiKey
        if (apiKey.isEmpty()) {
            tvLatestPlan.text = "Please configure ChatGPT API key in Settings"
            return
        }
        
        tvLatestPlan.text = getString(R.string.analyzing)
        
        lifecycleScope.launch {
            try {
                val countries = prefsManager.countries
                val language = prefsManager.language
                val chatGPTService = ChatGPTService(apiKey)
                
                val result = chatGPTService.analyzeStockImage(lastImagePath, countries, language)
                
                result.onSuccess { planContent ->
                    planManager.savePlan(planContent, lastImagePath)
                    loadPlans()
                }.onFailure { error ->
                    tvLatestPlan.text = getString(R.string.analysis_failed, error.message)
                }
            } catch (e: Exception) {
                tvLatestPlan.text = getString(R.string.analysis_failed, e.message)
            }
        }
    }
}
