package com.autostockplan.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var planManager: ExecutionPlanManager
    private lateinit var tvLatestPlan: TextView
    private lateinit var btnRefresh: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: ExecutionPlanAdapter
    private var selectedAnalysisCountry: String? = null
    private var isAnalyzing = false
    private val analyzingHandler = Handler(Looper.getMainLooper())
    private var analyzingFrame = 0
    private val analyzingMessages = listOf(
        "Reading the screenshot",
        "Finding holdings and symbols",
        "Checking market context",
        "Building the execution plan"
    )
    private val analyzingRunnable = object : Runnable {
        override fun run() {
            val message = analyzingMessages[analyzingFrame % analyzingMessages.size]
            val dots = ".".repeat((analyzingFrame % 4) + 1)
            tvLatestPlan.text = "$message$dots\n\nPlease keep this screen open while the analysis runs."
            analyzingFrame++
            analyzingHandler.postDelayed(this, 500L)
        }
    }
    
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
        btnRefresh = findViewById(R.id.btnRefresh)
        fab = findViewById(R.id.fab)
        rvHistory = findViewById(R.id.rvHistory)
        
        btnRefresh.setOnClickListener {
            startAnalysis()
        }
        
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        fab.setOnClickListener {
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
        if (!isAnalyzing) {
            loadPlans()
        }
    }

    override fun onDestroy() {
        analyzingHandler.removeCallbacks(analyzingRunnable)
        super.onDestroy()
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
        if (isAnalyzing) return

        if (!prefsManager.isConfigured()) {
            checkConfiguration()
            return
        }
        showCountryPicker()
    }

    private fun showCountryPicker() {
        val countryOptions = resources.getStringArray(R.array.stock_country_options)
        val defaultCountry = prefsManager.countries.takeIf { it in countryOptions } ?: countryOptions.first()
        var selectedIndex = countryOptions.indexOf(defaultCountry).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_country)
            .setSingleChoiceItems(countryOptions, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(R.string.start_analysis) { _, _ ->
                selectedAnalysisCountry = countryOptions[selectedIndex]
                selectImageLauncher.launch("image/*")
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
            runAnalysis(file.absolutePath, selectedAnalysisCountry ?: prefsManager.countries)
        } catch (e: Exception) {
            Toast.makeText(this, "Error selecting image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun runAnalysis(imagePath: String, countries: String) {
        val provider = prefsManager.modelProvider
        val apiKey = prefsManager.getApiToken(provider)
        startAnalyzingAnimation()
        
        lifecycleScope.launch {
            try {
                val language = prefsManager.language
                val model = prefsManager.getModel(provider)
                val chatGPTService = ChatGPTService(apiKey)
                
                val result = chatGPTService.analyzeStockImage(imagePath, countries, language, model, provider)
                
                result.onSuccess { planContent ->
                    planManager.savePlan(planContent, imagePath, countries, language, model)
                    stopAnalyzingAnimation()
                    loadPlans()
                }.onFailure { error ->
                    stopAnalyzingAnimation()
                    tvLatestPlan.text = getString(R.string.analysis_failed, error.message)
                }
            } catch (e: Exception) {
                stopAnalyzingAnimation()
                tvLatestPlan.text = getString(R.string.analysis_failed, e.message)
            }
        }
    }

    private fun startAnalyzingAnimation() {
        isAnalyzing = true
        btnRefresh.isEnabled = false
        fab.isEnabled = false
        fab.alpha = 0.45f
        tvLatestPlan.setOnClickListener(null)
        analyzingFrame = 0
        analyzingHandler.removeCallbacks(analyzingRunnable)
        analyzingHandler.post(analyzingRunnable)
    }

    private fun stopAnalyzingAnimation() {
        isAnalyzing = false
        analyzingHandler.removeCallbacks(analyzingRunnable)
        btnRefresh.isEnabled = true
        fab.isEnabled = true
        fab.alpha = 1f
    }
}
