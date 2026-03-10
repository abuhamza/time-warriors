package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.model.ExcludedDateRange
import com.timewgui.domain.model.ExclusionSource
import com.timewgui.ui.navigation.Screen
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import java.util.prefs.Preferences
import kotlin.time.Clock

class AppState {
    private val prefs = Preferences.userNodeForPackage(AppState::class.java)
    private val json = Json { ignoreUnknownKeys = true }

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

    var dailyTargetHours: Int by mutableStateOf(prefs.getInt(PREF_DAILY_TARGET, 8))

    var weeklyTargetHours: Int by mutableStateOf(prefs.getInt(PREF_WEEKLY_TARGET, 40))

    var overtimeEnabled: Boolean by mutableStateOf(prefs.getBoolean(PREF_OVERTIME_ENABLED, false))

    var overtimeStartDate: LocalDate by mutableStateOf(loadOvertimeStartDate())

    var workdays: Set<DayOfWeek> by mutableStateOf(loadWorkdays())

    var defaultContextTags: List<String> by mutableStateOf(loadDefaultContextTags())

    var excludedDateRanges: List<ExcludedDateRange> by mutableStateOf(loadExcludedDateRanges())

    var absenceIoEnabled: Boolean by mutableStateOf(prefs.getBoolean(PREF_ABSENCE_IO_ENABLED, false))
    var absenceIoKeyId: String by mutableStateOf(prefs.get(PREF_ABSENCE_IO_KEY_ID, ""))
    var absenceIoKeySecret: String by mutableStateOf(prefs.get(PREF_ABSENCE_IO_KEY_SECRET, ""))
    var absenceIoLastSync: String by mutableStateOf(prefs.get(PREF_ABSENCE_IO_LAST_SYNC, ""))

    var apiBaseUrl: String by mutableStateOf(prefs.get(PREF_API_BASE_URL, "https://ai.aitconsulting.agency"))
    var apiToken: String by mutableStateOf(prefs.get(PREF_API_TOKEN, ""))

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

    fun updateDailyTargetHours(hours: Int) {
        dailyTargetHours = hours.coerceIn(1, 24)
        prefs.putInt(PREF_DAILY_TARGET, dailyTargetHours)
    }

    fun updateWeeklyTargetHours(hours: Int) {
        weeklyTargetHours = hours.coerceIn(1, 168)
        prefs.putInt(PREF_WEEKLY_TARGET, weeklyTargetHours)
    }

    fun updateOvertimeEnabled(enabled: Boolean) {
        overtimeEnabled = enabled
        prefs.putBoolean(PREF_OVERTIME_ENABLED, enabled)
    }

    fun updateOvertimeStartDate(date: LocalDate) {
        overtimeStartDate = date
        prefs.put(PREF_OVERTIME_START_DATE, date.toString())
    }

    fun updateWorkdays(days: Set<DayOfWeek>) {
        workdays = days
        prefs.put(PREF_WORKDAYS, days.joinToString(",") { (it.ordinal + 1).toString() })
    }

    fun updateDefaultContextTags(tags: List<String>) {
        defaultContextTags = tags
        prefs.put(PREF_DEFAULT_CONTEXT_TAGS, tags.joinToString(","))
        prefs.flush()
    }

    fun addExcludedDateRange(range: ExcludedDateRange) {
        excludedDateRanges = excludedDateRanges + range
        persistExcludedDateRanges()
    }

    fun removeExcludedDateRange(range: ExcludedDateRange) {
        excludedDateRanges = excludedDateRanges - range
        persistExcludedDateRanges()
    }

    fun updateExcludedDateRanges(ranges: List<ExcludedDateRange>) {
        excludedDateRanges = ranges
        persistExcludedDateRanges()
    }

    fun updateAbsenceIoEnabled(enabled: Boolean) {
        absenceIoEnabled = enabled
        prefs.putBoolean(PREF_ABSENCE_IO_ENABLED, enabled)
    }

    fun updateAbsenceIoKeyId(id: String) {
        absenceIoKeyId = id
        prefs.put(PREF_ABSENCE_IO_KEY_ID, id)
    }

    fun updateAbsenceIoKeySecret(secret: String) {
        absenceIoKeySecret = secret
        prefs.put(PREF_ABSENCE_IO_KEY_SECRET, secret)
    }

    fun updateAbsenceIoLastSync(timestamp: String) {
        absenceIoLastSync = timestamp
        prefs.put(PREF_ABSENCE_IO_LAST_SYNC, timestamp)
    }

    fun updateApiBaseUrl(url: String) {
        apiBaseUrl = url
        prefs.put(PREF_API_BASE_URL, url)
    }

    fun updateApiToken(token: String) {
        apiToken = token
        prefs.put(PREF_API_TOKEN, token)
    }

    private fun loadOvertimeStartDate(): LocalDate {
        val raw = prefs.get(PREF_OVERTIME_START_DATE, "")
        return if (raw.isBlank()) {
            Clock.System.todayIn(TimeZone.currentSystemDefault())
        } else {
            runCatching { LocalDate.parse(raw) }
                .getOrElse { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
        }
    }

    private fun loadWorkdays(): Set<DayOfWeek> {
        val raw = prefs.get(PREF_WORKDAYS, "")
        return if (raw.isBlank()) {
            setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        } else {
            raw.split(",").mapNotNull { s ->
                val isoDay = s.trim().toIntOrNull() ?: return@mapNotNull null
                DayOfWeek.entries.getOrNull(isoDay - 1)
            }.toSet()
        }
    }

    private fun loadDefaultContextTags(): List<String> {
        val raw = prefs.get(PREF_DEFAULT_CONTEXT_TAGS, "")
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    private fun loadExcludedDateRanges(): List<ExcludedDateRange> {
        val raw = prefs.get(PREF_EXCLUDED_DATE_RANGES, "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<ExcludedDateRange>>(raw)
        }.getOrElse { emptyList() }
    }

    private fun persistExcludedDateRanges() {
        val encoded = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ExcludedDateRange.serializer()), excludedDateRanges)
        prefs.put(PREF_EXCLUDED_DATE_RANGES, encoded)
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
        private const val PREF_DAILY_TARGET = "daily_target_hours"
        private const val PREF_WEEKLY_TARGET = "weekly_target_hours"
        private const val PREF_DEFAULT_CONTEXT_TAGS = "default_context_tags"
        private const val PREF_OVERTIME_ENABLED = "overtime_enabled"
        private const val PREF_OVERTIME_START_DATE = "overtime_start_date"
        private const val PREF_WORKDAYS = "workdays"
        private const val PREF_EXCLUDED_DATE_RANGES = "excluded_date_ranges"
        private const val PREF_ABSENCE_IO_ENABLED = "absence_io_enabled"
        private const val PREF_ABSENCE_IO_KEY_ID = "absence_io_key_id"
        private const val PREF_ABSENCE_IO_KEY_SECRET = "absence_io_key_secret"
        private const val PREF_ABSENCE_IO_LAST_SYNC = "absence_io_last_sync"
        private const val PREF_API_BASE_URL = "api_base_url"
        private const val PREF_API_TOKEN = "api_token"
    }
}
