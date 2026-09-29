package com.bhenx.finder.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference {
    DARK,
    LIGHT,
    SYSTEM
}

enum class LanguagePreference(val code: String, val displayName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    CREOLE("ht", "Kreyòl")
}

/**
 * Gestion locale et persistante des paramètres de l'application via SharedPreferences.
 * Pas de serveur, 100% autonome et local.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _deviceName = MutableStateFlow(
        prefs.getString(KEY_DEVICE_NAME, DEFAULT_DEVICE_NAME) ?: DEFAULT_DEVICE_NAME
    )
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _theme = MutableStateFlow(
        parseTheme(prefs.getString(KEY_THEME, ThemePreference.DARK.name))
    )
    val theme: StateFlow<ThemePreference> = _theme.asStateFlow()

    private val _language = MutableStateFlow(
        parseLanguage(prefs.getString(KEY_LANGUAGE, LanguagePreference.FRENCH.code))
    )
    val language: StateFlow<LanguagePreference> = _language.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_NOTIFICATIONS, true)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setDeviceName(newName: String) {
        val trimmed = newName.trim().ifBlank { DEFAULT_DEVICE_NAME }
        prefs.edit().putString(KEY_DEVICE_NAME, trimmed).apply()
        _deviceName.value = trimmed
    }

    fun setTheme(themePreference: ThemePreference) {
        prefs.edit().putString(KEY_THEME, themePreference.name).apply()
        _theme.value = themePreference
    }

    fun setLanguage(languagePreference: LanguagePreference) {
        prefs.edit().putString(KEY_LANGUAGE, languagePreference.code).apply()
        _language.value = languagePreference
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    private fun parseTheme(name: String?): ThemePreference {
        return try {
            ThemePreference.valueOf(name ?: ThemePreference.DARK.name)
        } catch (_: Exception) {
            ThemePreference.DARK
        }
    }

    private fun parseLanguage(code: String?): LanguagePreference {
        return when (code) {
            "en" -> LanguagePreference.ENGLISH
            "ht" -> LanguagePreference.CREOLE
            else -> LanguagePreference.FRENCH
        }
    }

    companion object {
        private const val PREFS_NAME = "bhenx_finder_settings"
        private const val KEY_DEVICE_NAME = "key_device_name"
        private const val KEY_THEME = "key_theme"
        private const val KEY_LANGUAGE = "key_language"
        private const val KEY_NOTIFICATIONS = "key_notifications"
        const val DEFAULT_DEVICE_NAME = "BHENX FINDER"
    }
}
