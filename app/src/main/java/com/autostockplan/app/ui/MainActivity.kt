package com.autostockplan.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var planManager: ExecutionPlanManager
    private lateinit var tvLatestPlan: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: ExecutionPlanAdapter
    
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        
        prefsManager = PreferencesManager(this)
        planManager = ExecutionPlanManager(this)
        
        tvLatestPlan = findViewById(R.id.tvLatestPlan)
        rvHistory = findViewById(R.id.rvHistory)
        
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            startAnalysis()
        }
        
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startAnalysis()
        }
        
        rvHistory.layoutManager = LinearLayoutManager(this)
        
        // Start the monitoring service (still available if user wants auto-monitor)
        startForegroundService(Intent(this, FolderMonitorService::class.java))
        
        checkConfiguration()
        loadPlans()
    }
    
    override fun onResume() {
        super.onResume()
        loadPlans()
    }
    
    private fun checkConfiguration() {
        if (!prefsManager.isConfigured()) {
            Toast.makeText(this, R.string.config_required, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
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
    
    private fun startAnalysis() {
        if (!prefsManager.isConfigured()) {
            checkConfiguration()
            return
        }
        selectImageLauncher.launch("image/*")
    }
    
    private fun handleSelectedImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(getExternalFilesDir(null), "analysis_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            runAnalysis(file.absolutePath)
        } catch (e: Exception) {
            Toast.makeText(this, "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun runAnalysis(imagePath: String) {
        val apiKey = prefsManager.chatGptApiKey
        tvLatestPlan.text = getString(R.string.analyzing)
        
        lifecycleScope.launch {
            try {
                val countries = prefsManager.countries
                val language = prefsManager.language
                val model = prefsManager.model
                val chatGPTService = ChatGPTService(apiKey)
                
                val result = chatGPTService.analyzeStockImage(imagePath, countries, language, model)
                
                result.onSuccess { planContent ->
                    planManager.savePlan(planContent, imagePath, countries, language, model)
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
