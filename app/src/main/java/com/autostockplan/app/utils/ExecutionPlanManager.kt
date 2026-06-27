package com.autostockplan.app.utils

import android.content.Context
import com.autostockplan.app.data.ExecutionPlan
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExecutionPlanManager(context: Context) {
    private val plansDir: File = File(context.getExternalFilesDir(null), "execution_plans")
    private val gson = Gson()

    init {
        if (!plansDir.exists()) {
            plansDir.mkdirs()
        }
    }

    fun savePlan(
        content: String,
        imagePath: String?,
        countries: String,
        language: String,
        model: String
    ): String {
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "${dateTime}.txt"
        val file = File(plansDir, filename)
        
        file.writeText(content)
        
        val plan = ExecutionPlan(
            id = dateTime,
            content = content,
            imagePath = imagePath,
            countries = countries,
            language = language,
            model = model,
            createdAt = System.currentTimeMillis()
        )
        
        val metadataFile = File(plansDir, "${dateTime}_meta.json")
        metadataFile.writeText(gson.toJson(plan))
        
        return filename
    }

    fun loadPlan(filename: String): ExecutionPlan? {
        val file = File(plansDir, filename)
        if (!file.exists()) return null
        
        val dateTimeId = filename.replace(".txt", "")
        val metadataFile = File(plansDir, "${dateTimeId}_meta.json")
        
        return if (metadataFile.exists()) {
            try {
                gson.fromJson(metadataFile.readText(), ExecutionPlan::class.java)
            } catch (e: Exception) {
                // Fallback for old format
                ExecutionPlan(id = dateTimeId, content = file.readText())
            }
        } else {
            ExecutionPlan(id = dateTimeId, content = file.readText())
        }
    }

    fun getAllPlanIds(): List<String> {
        return plansDir.listFiles()
            ?.filter { it.name.endsWith(".txt") }
            ?.map { it.name.replace(".txt", "") }
            ?.sortedDescending() ?: emptyList()
    }

    fun getLatestPlan(): ExecutionPlan? {
        val planIds = getAllPlanIds()
        return if (planIds.isNotEmpty()) {
            loadPlan("${planIds.first()}.txt")
        } else null
    }

    fun deletePlan(filename: String): Boolean {
        val file = File(plansDir, filename)
        val metadataFile = File(plansDir, filename.replace(".txt", "_meta.json"))
        file.delete()
        metadataFile.delete()
        return true
    }
}
