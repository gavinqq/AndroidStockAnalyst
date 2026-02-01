package com.autostockplan.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

class ImageDetector {
    
    fun isStockHoldingImage(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (!file.exists() || !file.isFile) return false
            
            // Check if it's an image file
            val extension = file.extension.lowercase()
            if (!listOf("jpg", "jpeg", "png", "webp").contains(extension)) {
                return false
            }
            
            // Basic heuristics to detect stock holding images
            // 1. Check file size (stock screenshots are typically larger)
            if (file.length() < 10000) return false // Less than 10KB probably not a screenshot
            
            // 2. Check image dimensions (stock apps usually show full screen or wide format)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)
            
            val width = options.outWidth
            val height = options.outHeight
            
            if (width <= 0 || height <= 0) return false
            
            // Stock images are typically wide or tall (phone screenshots)
            val aspectRatio = width.toFloat() / height.toFloat()
            val isReasonableAspectRatio = aspectRatio > 0.5 && aspectRatio < 3.0
            
            // 3. Check if image has reasonable dimensions (not too small)
            val hasReasonableSize = width >= 300 && height >= 300
            
            // 4. Check filename for common stock-related keywords (optional hint)
            val filename = file.name.lowercase()
            val hasStockKeywords = listOf(
                "stock", "holding", "portfolio", "trade", "market",
                "screenshot", "stock", "account"
            ).any { filename.contains(it) }
            
            // Consider it a stock image if it passes basic checks
            // The ChatGPT API will do the actual analysis anyway
            isReasonableAspectRatio && hasReasonableSize
            
        } catch (e: Exception) {
            false
        }
    }
}
