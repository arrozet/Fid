package com.example.fid.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Helper class to manage application language/locale changes
 */
object LocaleHelper {
    
    private const val PREFS_NAME = "fid_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    // Supported languages
    const val LANGUAGE_SPANISH = "es"
    const val LANGUAGE_ENGLISH = "en"
    
    /**
     * Get the saved language preference, defaults to Spanish
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SPANISH) ?: LANGUAGE_SPANISH
    }
    
    /**
     * Alias for getLanguage - gets the current language
     */
    fun getCurrentLanguage(context: Context): String {
        return getLanguage(context)
    }
    
    /**
     * Save the language preference
     */
    fun setLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    /**
     * Apply the saved language to the context
     * Call this in Application.attachBaseContext() and Activity.attachBaseContext()
     */
    fun applyLanguage(context: Context): Context {
        val language = getLanguage(context)
        return updateResources(context, language)
    }
    
    /**
     * Update the context with the specified language
     */
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Get the display name for a language code
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_SPANISH -> "Español"
            LANGUAGE_ENGLISH -> "English"
            else -> languageCode
        }
    }
    
    /**
     * Get all supported languages as pairs of (code, displayName)
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            LANGUAGE_SPANISH to "Español",
            LANGUAGE_ENGLISH to "English"
        )
    }
}
