package com.autostockplan.app.api

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ChatGPTService(private val apiKey: String) {
    
    companion object {
        private const val API_URL = "https://api.openai.com/v1/chat/completions"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun analyzeStockImage(
        imagePath: String,
        countries: String,
        language: String = "English",
        model: String = "gpt-4o"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageBase64 = encodeImageToBase64(imagePath)
            if (imageBase64 == null) {
                return@withContext Result.failure(IOException("Failed to encode image"))
            }

            val nextWorkDay = getNextWorkDay()
            val prompt = buildPrompt(countries, nextWorkDay, language)

            val requestBody = buildRequestBody(imageBase64, prompt, model)
            val request = Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val content = message.getString("content")
                    Result.success(content)
                } else {
                    Result.failure(IOException("No response from ChatGPT"))
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(IOException("API error: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encodeImageToBase64(imagePath: String): String? {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return null
            
            val bytes = imageFile.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPrompt(countries: String, nextWorkDay: String, language: String): String {
        return """
            Analyze this stock holding image. Based on the stock markets in these countries: $countries,
            provide a detailed execution plan for trading activities on the next work day: $nextWorkDay.
            
            Please include:
            1. Current holdings analysis
            2. Recommended buy/sell actions
            3. Entry/exit prices
            4. Stop-loss and take-profit levels
            5. Risk management notes
            6. Market outlook for the specified countries
            7. Analysis history can be found, which includes the screenshot used for analysis, the market, the anaysis time and analysis result are recorded.
            
            IMPORTANT: Please write the entire execution plan in $language language.
            Format the response in a clear, actionable execution plan.
        """.trimIndent()
    }

    private fun buildRequestBody(imageBase64: String, prompt: String, model: String): RequestBody {
        val jsonBody = JSONObject().apply {
            put("model", model)
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$imageBase64")
                            })
                        })
                    })
                })
            })
            put("max_tokens", 2000)
        }
        
        return jsonBody.toString().toRequestBody(JSON)
    }

    private fun getNextWorkDay(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        
        // Skip weekends
        while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
               calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return SimpleDateFormat("yyyy-MM-dd (EEEE)", Locale.getDefault()).format(calendar.time)
    }
}
