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
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions"
        private const val QWEN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        private const val DOUBAO_API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions"
        private const val GEMINI_API_URL_PREFIX = "https://generativelanguage.googleapis.com/v1beta/models/"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun analyzeStockImage(
        imagePath: String,
        countries: String,
        language: String = "English",
        model: String = "gpt-4o",
        provider: String = "ChatGPT"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val imageBase64 = encodeImageToBase64(imagePath)
            if (imageBase64 == null) {
                return@withContext Result.failure(IOException("Failed to encode image"))
            }

            val nextWorkDay = getNextWorkDay()
            val prompt = buildPrompt(countries, nextWorkDay, language)

            val request = buildRequest(imageBase64, prompt, model, provider)

            val client = OkHttpClient()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Result.success(parseResponse(responseBody ?: "", provider))
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Result.failure(IOException("API error: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequest(
        imageBase64: String,
        prompt: String,
        model: String,
        provider: String
    ): Request {
        if (provider == "Gemini") {
            return Request.Builder()
                .url("$GEMINI_API_URL_PREFIX$model:generateContent?key=$apiKey")
                .header("Content-Type", "application/json")
                .post(buildGeminiRequestBody(imageBase64, prompt))
                .build()
        }

        val apiUrl = when (provider) {
            "Deepseek" -> DEEPSEEK_API_URL
            "QWEN" -> QWEN_API_URL
            "Doubao" -> DOUBAO_API_URL
            else -> OPENAI_API_URL
        }

        return Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(buildOpenAiCompatibleRequestBody(imageBase64, prompt, model))
            .build()
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
        if (countries.trim().equals("US", ignoreCase = true)) {
            return """
                Analyze this stock holding image. Based on the stock markets in these countries: $countries,
                provide an overview trend for of the next work day: $nextWorkDay.
                
                Please include:
                1. Current holdings analysis
                2. Stop-loss and take-profit levels
                3. Risk management notes
                4. Market outlook for the specified countries
                5. Analysis history can be found, which includes the screenshot used for analysis, the market, the anaysis time and analysis result are recorded.
                
                IMPORTANT: Please write in $language language.
                Format the response in a clear plan.
            """.trimIndent()
        }

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

    private fun buildOpenAiCompatibleRequestBody(imageBase64: String, prompt: String, model: String): RequestBody {
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

    private fun buildGeminiRequestBody(imageBase64: String, prompt: String): RequestBody {
        val jsonBody = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", imageBase64)
                            })
                        })
                    })
                })
            })
        }

        return jsonBody.toString().toRequestBody(JSON)
    }

    private fun parseResponse(responseBody: String, provider: String): String {
        val jsonResponse = JSONObject(responseBody)
        if (provider == "Gemini") {
            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) throw IOException("No response from $provider")
            val parts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            if (parts.length() == 0) throw IOException("No response from $provider")
            return parts.getJSONObject(0).getString("text")
        }

        val choices = jsonResponse.getJSONArray("choices")
        if (choices.length() == 0) throw IOException("No response from $provider")
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.getString("content")
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
