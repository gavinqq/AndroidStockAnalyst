package com.autostockplan.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autostockplan.app.R
import com.autostockplan.app.utils.PreferencesManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var etCountries: AutoCompleteTextView
    private lateinit var etApiKey: TextInputEditText
    private lateinit var etLanguage: AutoCompleteTextView
    private lateinit var etOtherLanguage: TextInputEditText
    private lateinit var etProvider: AutoCompleteTextView
    private lateinit var etModel: AutoCompleteTextView
    private lateinit var layoutOtherLanguage: TextInputLayout
    private lateinit var tvOtherLanguage: TextView
    private lateinit var tvOtherLanguageHint: TextView
    private lateinit var providerOptions: Array<String>
    private lateinit var modelOptions: Array<String>
    private lateinit var countryOptions: Array<String>
    private lateinit var languageOptions: Array<String>
    private var currentProvider: String = "ChatGPT"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        prefsManager = PreferencesManager(this)
        
        etCountries = findViewById(R.id.etCountries)
        etApiKey = findViewById(R.id.etApiKey)
        etLanguage = findViewById(R.id.etLanguage)
        etOtherLanguage = findViewById(R.id.etOtherLanguage)
        etProvider = findViewById(R.id.etProvider)
        etModel = findViewById(R.id.etModel)
        layoutOtherLanguage = findViewById(R.id.layoutOtherLanguage)
        tvOtherLanguage = findViewById(R.id.tvOtherLanguage)
        tvOtherLanguageHint = findViewById(R.id.tvOtherLanguageHint)
        providerOptions = resources.getStringArray(R.array.model_provider_options)
        countryOptions = resources.getStringArray(R.array.stock_country_options)
        languageOptions = resources.getStringArray(R.array.language_options)
        currentProvider = resolveProvider(prefsManager.modelProvider)

        etProvider.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, providerOptions))
        etCountries.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, countryOptions))
        etLanguage.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, languageOptions))
        
        // Load current settings
        etProvider.setText(currentProvider, false)
        loadProviderSettings(currentProvider)
        etCountries.setText(resolveCountry(prefsManager.countries), false)
        val languageSelection = resolveLanguageSelection(prefsManager.languageSelection)
        etLanguage.setText(languageSelection, false)
        etOtherLanguage.setText(prefsManager.customLanguage)
        updateOtherLanguageVisibility(languageSelection)

        etProvider.setOnItemClickListener { _, _, position, _ ->
            saveProviderFields(currentProvider)
            currentProvider = providerOptions[position]
            loadProviderSettings(currentProvider)
        }

        etLanguage.setOnItemClickListener { _, _, position, _ ->
            updateOtherLanguageVisibility(languageOptions[position])
        }
        
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            saveSettings()
        }
    }
    
    private fun saveSettings() {
        val provider = resolveProvider(etProvider.text?.toString()?.trim())
        val countries = resolveCountry(etCountries.text?.toString()?.trim())
        val apiKey = etApiKey.text?.toString()?.trim() ?: ""
        val languageSelection = resolveLanguageSelection(etLanguage.text?.toString()?.trim())
        val customLanguage = etOtherLanguage.text?.toString()?.trim() ?: ""
        val model = resolveModel(etModel.text?.toString()?.trim())
        
        if (countries.isEmpty()) {
            Toast.makeText(this, "Countries cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (languageSelection == "Other" && customLanguage.isEmpty()) {
            Toast.makeText(this, "Language cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefsManager.modelProvider = provider
        prefsManager.countries = countries
        prefsManager.setApiToken(provider, apiKey)
        prefsManager.setModel(provider, model)
        prefsManager.languageSelection = languageSelection
        prefsManager.customLanguage = customLanguage
        
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resolveModel(model: String?): String {
        val selectedModel = model?.takeIf { it in modelOptions }
        return selectedModel ?: modelOptions.first()
    }

    private fun resolveProvider(provider: String?): String {
        val selectedProvider = provider?.takeIf { it in providerOptions }
        return selectedProvider ?: providerOptions.first()
    }

    private fun resolveCountry(country: String?): String {
        val selectedCountry = country?.takeIf { it in countryOptions }
        return selectedCountry ?: countryOptions.first()
    }

    private fun resolveLanguageSelection(language: String?): String {
        val selectedLanguage = language?.takeIf { it in languageOptions }
        return selectedLanguage ?: "Other"
    }

    private fun loadProviderSettings(provider: String) {
        modelOptions = modelOptionsForProvider(provider)
        etModel.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, modelOptions))
        etApiKey.setText(prefsManager.getApiToken(provider))
        etModel.setText(resolveModel(prefsManager.getModel(provider)), false)
    }

    private fun saveProviderFields(provider: String) {
        val token = etApiKey.text?.toString()?.trim() ?: ""
        val model = etModel.text?.toString()?.trim() ?: ""
        prefsManager.setApiToken(provider, token)
        if (model.isNotEmpty()) {
            prefsManager.setModel(provider, model)
        }
    }

    private fun updateOtherLanguageVisibility(languageSelection: String) {
        val visibility = if (languageSelection == "Other") View.VISIBLE else View.GONE
        tvOtherLanguage.visibility = visibility
        layoutOtherLanguage.visibility = visibility
        tvOtherLanguageHint.visibility = visibility
    }

    private fun modelOptionsForProvider(provider: String): Array<String> {
        val arrayId = when (provider) {
            "Deepseek" -> R.array.deepseek_model_options
            "Gemini" -> R.array.gemini_model_options
            "QWEN" -> R.array.qwen_model_options
            "Doubao" -> R.array.doubao_model_options
            else -> R.array.openai_model_options
        }
        return resources.getStringArray(arrayId)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
