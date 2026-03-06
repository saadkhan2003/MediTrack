package com.meditrack.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_preferences")

data class AppPreferencesState(
    val notificationsEnabled: Boolean = true,
    val reminderLeadMinutes: Int = 0,
    val themeMode: String = "SYSTEM",
    val fontSize: String = "NORMAL",
    val batteryOptimizationPromptShown: Boolean = false
)

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val state: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        AppPreferencesState(
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            reminderLeadMinutes = prefs[KEY_REMINDER_LEAD_MINUTES] ?: 0,
            themeMode = prefs[KEY_THEME_MODE] ?: "SYSTEM",
            fontSize = prefs[KEY_FONT_SIZE] ?: "NORMAL",
            batteryOptimizationPromptShown = prefs[KEY_BATTERY_PROMPT_SHOWN] ?: false
        )
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun setReminderLeadMinutes(value: Int) {
        context.dataStore.edit { it[KEY_REMINDER_LEAD_MINUTES] = value }
    }

    suspend fun setThemeMode(value: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = value }
    }

    suspend fun setFontSize(value: String) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = value }
    }

    suspend fun setBatteryOptimizationPromptShown(value: Boolean) {
        context.dataStore.edit { it[KEY_BATTERY_PROMPT_SHOWN] = value }
    }

    suspend fun reset() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_NOTIFICATIONS_ENABLED)
            prefs.remove(KEY_REMINDER_LEAD_MINUTES)
            prefs.remove(KEY_THEME_MODE)
            prefs.remove(KEY_FONT_SIZE)
            prefs.remove(KEY_BATTERY_PROMPT_SHOWN)
        }
    }

    private companion object {
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_FONT_SIZE = stringPreferencesKey("font_size")
        val KEY_BATTERY_PROMPT_SHOWN = booleanPreferencesKey("battery_prompt_shown")
    }
}
