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
        private const val KEY_LANGUAGE_SELECTION = "language_selection"
        private const val KEY_CUSTOM_LANGUAGE = "custom_language"
        private const val KEY_MODEL = "model"
        private const val KEY_MODEL_PROVIDER = "model_provider"
        private const val KEY_LAST_IMAGE_PATH = "last_image_path"
        private const val DEFAULT_PROVIDER = "ChatGPT"
    }

    var folderPath: String
        get() = prefs.getString(KEY_FOLDER_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FOLDER_PATH, value).apply()

    var countries: String
        get() = prefs.getString(KEY_COUNTRIES, "") ?: ""
        set(value) = prefs.edit().putString(KEY_COUNTRIES, value).apply()

    var chatGptApiKey: String
        get() = getApiToken(DEFAULT_PROVIDER)
        set(value) = prefs.edit()
            .putString(KEY_CHATGPT_API_KEY, value)
            .putString(apiTokenKey(DEFAULT_PROVIDER), value)
            .apply()

    var language: String
        get() {
            val selection = languageSelection
            return if (selection == "Other") customLanguage.ifEmpty { "English" } else selection
        }
        set(value) {
            prefs.edit()
                .putString(KEY_LANGUAGE, value)
                .putString(KEY_LANGUAGE_SELECTION, value)
                .apply()
        }

    var languageSelection: String
        get() = prefs.getString(KEY_LANGUAGE_SELECTION, prefs.getString(KEY_LANGUAGE, "English")) ?: "English"
        set(value) = prefs.edit().putString(KEY_LANGUAGE_SELECTION, value).apply()

    var customLanguage: String
        get() = prefs.getString(KEY_CUSTOM_LANGUAGE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_LANGUAGE, value).apply()
        
    var model: String
        get() = getModel(modelProvider).ifEmpty { prefs.getString(KEY_MODEL, "gpt-4o") ?: "gpt-4o" }
        set(value) = setModel(modelProvider, value)

    var modelProvider: String
        get() = prefs.getString(KEY_MODEL_PROVIDER, DEFAULT_PROVIDER) ?: DEFAULT_PROVIDER
        set(value) = prefs.edit().putString(KEY_MODEL_PROVIDER, value).apply()

    var lastImagePath: String
        get() = prefs.getString(KEY_LAST_IMAGE_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_IMAGE_PATH, value).apply()

    fun getApiToken(provider: String): String {
        val savedToken = prefs.getString(apiTokenKey(provider), "") ?: ""
        return if (savedToken.isNotEmpty() || provider != DEFAULT_PROVIDER) {
            savedToken
        } else {
            prefs.getString(KEY_CHATGPT_API_KEY, "") ?: ""
        }
    }

    fun setApiToken(provider: String, value: String) {
        prefs.edit()
            .putString(apiTokenKey(provider), value)
            .also {
                if (provider == DEFAULT_PROVIDER) {
                    it.putString(KEY_CHATGPT_API_KEY, value)
                }
            }
            .apply()
    }

    fun getModel(provider: String): String {
        val savedModel = prefs.getString(modelKey(provider), "") ?: ""
        return if (savedModel.isNotEmpty() || provider != DEFAULT_PROVIDER) {
            savedModel
        } else {
            prefs.getString(KEY_MODEL, "gpt-4o") ?: "gpt-4o"
        }
    }

    fun setModel(provider: String, value: String) {
        prefs.edit()
            .putString(modelKey(provider), value)
            .also {
                if (provider == DEFAULT_PROVIDER) {
                    it.putString(KEY_MODEL, value)
                }
            }
            .apply()
    }
        
    fun isConfigured(): Boolean {
        return countries.isNotEmpty() && getApiToken(modelProvider).isNotEmpty() && language.isNotEmpty()
    }

    private fun apiTokenKey(provider: String) = "api_token_$provider"

    private fun modelKey(provider: String) = "model_$provider"
}
