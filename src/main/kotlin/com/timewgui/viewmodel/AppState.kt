package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.cli.TimewCli
import com.timewgui.ui.navigation.Screen
import java.util.prefs.Preferences

class AppState {
    private val prefs = Preferences.userNodeForPackage(AppState::class.java)

    var currentScreen: Screen by mutableStateOf(Screen.DASHBOARD)
        private set

    var sidebarExpanded: Boolean by mutableStateOf(true)
        private set

    var isDarkTheme: Boolean? by mutableStateOf(null)
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    var idleDetectionEnabled: Boolean by mutableStateOf(prefs.getBoolean(PREF_IDLE_ENABLED, true))

    var idleThresholdMinutes: Int by mutableStateOf(prefs.getInt(PREF_IDLE_THRESHOLD, 5))

    var launchAtLogin: Boolean by mutableStateOf(prefs.getBoolean(PREF_LAUNCH_AT_LOGIN, false))

    var defaultContextTags: List<String> by mutableStateOf(loadDefaultContextTags())

    val timewCli: TimewCli = TimewCli()

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun toggleSidebar() {
        sidebarExpanded = !sidebarExpanded
    }

    fun updateSidebarExpanded(expanded: Boolean) {
        sidebarExpanded = expanded
    }

    fun updateDarkTheme(followSystem: Boolean? = null) {
        isDarkTheme = followSystem
    }

    fun setDarkThemeOverride(dark: Boolean) {
        isDarkTheme = dark
    }

    fun updateIdleDetectionEnabled(enabled: Boolean) {
        idleDetectionEnabled = enabled
        prefs.putBoolean(PREF_IDLE_ENABLED, enabled)
    }

    fun updateIdleThresholdMinutes(minutes: Int) {
        idleThresholdMinutes = minutes.coerceIn(1, 120)
        prefs.putInt(PREF_IDLE_THRESHOLD, idleThresholdMinutes)
    }

    fun updateLaunchAtLogin(enabled: Boolean) {
        launchAtLogin = enabled
        prefs.putBoolean(PREF_LAUNCH_AT_LOGIN, enabled)
    }

    fun updateDefaultContextTags(tags: List<String>) {
        defaultContextTags = tags
        prefs.put(PREF_DEFAULT_CONTEXT_TAGS, tags.joinToString(","))
        prefs.flush()
    }

    private fun loadDefaultContextTags(): List<String> {
        val raw = prefs.get(PREF_DEFAULT_CONTEXT_TAGS, "")
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun showError(message: String?) {
        errorMessage = message
    }

    fun clearError() {
        errorMessage = null
    }

    companion object {
        private const val PREF_IDLE_ENABLED = "idle_detection_enabled"
        private const val PREF_IDLE_THRESHOLD = "idle_threshold_minutes"
        private const val PREF_LAUNCH_AT_LOGIN = "launch_at_login"
        private const val PREF_DEFAULT_CONTEXT_TAGS = "default_context_tags"
    }
}
