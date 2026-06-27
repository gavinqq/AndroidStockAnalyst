package com.autostockplan.app.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "autostockplan_prefs"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_COUNTRIES = "countries"
        private const val KEY_CHATGPT_API_KEY = "chatgpt_api_key"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_MODEL = "model"
        private const val KEY_LAST_IMAGE_PATH = "last_image_path"
    }

    var folderPath: String
        get() = prefs.getString(KEY_FOLDER_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FOLDER_PATH, value).apply()

    var countries: String
        get() = prefs.getString(KEY_COUNTRIES, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COUNTRIES, value).apply()

    var chatGptApiKey: String
        get() = prefs.getString(KEY_CHATGPT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHATGPT_API_KEY, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()
        
    var model: String
        get() = prefs.getString(KEY_MODEL, "gpt-4o") ?: "gpt-4o"
        set(value) = prefs.edit().putString(KEY_MODEL, value).apply()

    var lastImagePath: String
        get() = prefs.getString(KEY_LAST_IMAGE_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_IMAGE_PATH, value).apply()
        
    fun isConfigured(): Boolean {
        return countries.isNotEmpty() && chatGptApiKey.isNotEmpty() && language.isNotEmpty()
    }
}
