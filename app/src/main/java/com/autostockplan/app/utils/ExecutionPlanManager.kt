package com.autostockplan.app.utils

import android.content.Context
import com.autostockplan.app.data.ExecutionPlan
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExecutionPlanManager(context: Context) {
    private val context: Context = context.applicationContext
    private val plansDir: File = File(context.getExternalFilesDir(null), "execution_plans")

    init {
        if (!plansDir.exists()) {
            plansDir.mkdirs()
        }
    }

    fun savePlan(content: String, imagePath: String?): String {
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "${dateTime}.txt"
        val file = File(plansDir, filename)
        
        file.writeText(content)
        
        // Save metadata if needed
        val metadataFile = File(plansDir, "${dateTime}_meta.json")
        val metadata = """{"imagePath":"${imagePath ?: ""}","createdAt":${System.currentTimeMillis()}}"""
        metadataFile.writeText(metadata)
        
        return filename
    }

    fun loadPlan(filename: String): ExecutionPlan? {
        val file = File(plansDir, filename)
        if (!file.exists()) return null
        
        val content = file.readText()
        val dateTimeId = filename.replace(".txt", "")
        
        // Try to load metadata
        val metadataFile = File(plansDir, "${dateTimeId}_meta.json")
        val imagePath = if (metadataFile.exists()) {
            try {
                val metadata = metadataFile.readText()
                // Simple JSON parsing for imagePath
                val imagePathMatch = Regex("\"imagePath\":\"([^\"]*)\"").find(metadata)
                imagePathMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                null
            }
        } else null
        
        return ExecutionPlan(
            id = dateTimeId,
            content = content,
            imagePath = imagePath
        )
    }

    fun getAllPlanIds(): List<String> {
        return plansDir.listFiles()
            ?.filter { it.name.endsWith(".txt") }
            ?.map { it.name.replace(".txt", "") }
            ?.sortedDescending() // Latest first
            ?: emptyList()
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
        
        val deleted = file.delete()
        metadataFile.delete()
        return deleted
    }
}
