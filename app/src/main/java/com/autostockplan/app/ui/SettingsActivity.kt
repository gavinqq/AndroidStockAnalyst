package com.autostockplan.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.autostockplan.app.R
import com.autostockplan.app.utils.PreferencesManager
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var prefsManager: PreferencesManager
    private lateinit var etCountries: TextInputEditText
    private lateinit var etApiKey: TextInputEditText
    private lateinit var etLanguage: TextInputEditText
    private lateinit var etModel: TextInputEditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        prefsManager = PreferencesManager(this)
        
        etCountries = findViewById(R.id.etCountries)
        etApiKey = findViewById(R.id.etApiKey)
        etLanguage = findViewById(R.id.etLanguage)
        etModel = findViewById(R.id.etModel)
        
        // Load current settings
        etCountries.setText(prefsManager.countries)
        etApiKey.setText(prefsManager.chatGptApiKey)
        etLanguage.setText(prefsManager.language)
        etModel.setText(prefsManager.model)
        
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            saveSettings()
        }
    }
    
    private fun saveSettings() {
        val countries = etCountries.text?.toString()?.trim() ?: ""
        val apiKey = etApiKey.text?.toString()?.trim() ?: ""
        val language = etLanguage.text?.toString()?.trim() ?: "English"
        val model = etModel.text?.toString()?.trim() ?: "gpt-4o"
        
        if (countries.isEmpty()) {
            Toast.makeText(this, "Countries cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefsManager.countries = countries
        prefsManager.chatGptApiKey = apiKey
        prefsManager.language = language
        prefsManager.model = model
        
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
