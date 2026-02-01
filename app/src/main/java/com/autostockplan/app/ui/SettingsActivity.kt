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
    private lateinit var etFolderPath: TextInputEditText
    private lateinit var etCountries: TextInputEditText
    private lateinit var etApiKey: TextInputEditText
    private lateinit var etLanguage: TextInputEditText
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        prefsManager = PreferencesManager(this)
        
        etFolderPath = findViewById(R.id.etFolderPath)
        etCountries = findViewById(R.id.etCountries)
        etApiKey = findViewById(R.id.etApiKey)
        etLanguage = findViewById(R.id.etLanguage)
        
        // Load current settings
        etFolderPath.setText(prefsManager.folderPath)
        etCountries.setText(prefsManager.countries)
        etApiKey.setText(prefsManager.chatGptApiKey)
        etLanguage.setText(prefsManager.language)
        
        findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            saveSettings()
        }
    }
    
    private fun saveSettings() {
        val folderPath = etFolderPath.text?.toString()?.trim() ?: ""
        val countries = etCountries.text?.toString()?.trim() ?: ""
        val apiKey = etApiKey.text?.toString()?.trim() ?: ""
        val language = etLanguage.text?.toString()?.trim() ?: "English"
        
        if (folderPath.isEmpty()) {
            Toast.makeText(this, "Folder path cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (countries.isEmpty()) {
            Toast.makeText(this, "Countries cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (apiKey.isEmpty()) {
            Toast.makeText(this, "ChatGPT API key cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        
        prefsManager.folderPath = folderPath
        prefsManager.countries = countries
        prefsManager.chatGptApiKey = apiKey
        prefsManager.language = language
        
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
