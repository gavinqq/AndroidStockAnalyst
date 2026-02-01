package com.autostockplan.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.autostockplan.app.R
import com.autostockplan.app.api.ChatGPTService
import com.autostockplan.app.data.ExecutionPlan
import com.autostockplan.app.ui.MainActivity
import com.autostockplan.app.utils.ExecutionPlanManager
import com.autostockplan.app.utils.ImageDetector
import com.autostockplan.app.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class FolderMonitorService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefsManager: PreferencesManager
    private lateinit var planManager: ExecutionPlanManager
    private lateinit var imageDetector: ImageDetector
    private val handler = Handler(Looper.getMainLooper())
    private val monitorInterval = 30000L // 30 seconds
    
    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkFolderForNewImages()
            handler.postDelayed(this, monitorInterval)
        }
    }
    
    private val processedImages = mutableSetOf<String>()
    
    companion object {
        private const val CHANNEL_ID = "FolderMonitorChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        planManager = ExecutionPlanManager(this)
        imageDetector = ImageDetector()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        handler.post(monitorRunnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Folder Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors download folder for stock images"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val folderPath = prefsManager.folderPath
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Stock Plan")
            .setContentText("Monitoring: $folderPath")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun checkFolderForNewImages() {
        val folderPath = prefsManager.folderPath
        val folder = File(folderPath)
        
        if (!folder.exists() || !folder.isDirectory) {
            return
        }

        val apiKey = prefsManager.chatGptApiKey
        if (apiKey.isEmpty()) {
            return // Cannot process without API key
        }

        val imageFiles = folder.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
        } ?: return

        for (imageFile in imageFiles) {
            val imagePath = imageFile.absolutePath
            if (imagePath in processedImages) continue

            // Check if this looks like a stock holding image
            if (imageDetector.isStockHoldingImage(imagePath)) {
                processedImages.add(imagePath)
                prefsManager.lastImagePath = imagePath
                
                // Process image asynchronously
                serviceScope.launch {
                    processStockImage(imagePath, apiKey)
                }
            }
        }
    }

    private suspend fun processStockImage(imagePath: String, apiKey: String) {
        try {
            val countries = prefsManager.countries
            val language = prefsManager.language
            val chatGPTService = ChatGPTService(apiKey)
            
            val result = chatGPTService.analyzeStockImage(imagePath, countries, language)
            
            result.onSuccess { planContent ->
                planManager.savePlan(planContent, imagePath)
                
                // Update notification
                val notificationManager = getSystemService(NotificationManager::class.java)
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Execution Plan Generated")
                    .setContentText("New plan created from image")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setAutoCancel(true)
                    .build()
                notificationManager.notify(NOTIFICATION_ID + 1, notification)
            }.onFailure { error ->
                // Log error but don't remove from processed images
                // so we can retry later if needed
                android.util.Log.e("FolderMonitorService", "Failed to process image: $error")
            }
        } catch (e: Exception) {
            android.util.Log.e("FolderMonitorService", "Error processing image", e)
        }
    }
}
