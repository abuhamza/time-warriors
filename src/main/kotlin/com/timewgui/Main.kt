package com.timewgui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.timewgui.ui.components.IdleDialog
import com.timewgui.ui.components.Sidebar
import com.timewgui.ui.components.StartTimerDialog
import com.timewgui.ui.components.TopBar
import com.timewgui.ui.navigation.Screen
import com.timewgui.ui.screens.DashboardScreen
import com.timewgui.ui.screens.ReportsScreen
import com.timewgui.ui.screens.SettingsScreen
import com.timewgui.ui.screens.TagsScreen
import com.timewgui.ui.screens.TasksScreen
import com.timewgui.ui.screens.TimelineScreen
import com.timewgui.ui.theme.TimewGuiTheme
import com.timewgui.domain.repository.TaskRepository
import com.timewgui.viewmodel.AppState
import com.timewgui.viewmodel.IdleViewModel
import com.timewgui.viewmodel.OvertimeViewModel
import com.timewgui.viewmodel.TagViewModel
import com.timewgui.viewmodel.TaskViewModel
import com.timewgui.viewmodel.TimelineViewModel
import com.timewgui.viewmodel.TimerViewModel

fun main() {
    // Set macOS dock icon before any AWT/Swing initialization
    try {
        val iconUrl = Thread.currentThread().contextClassLoader.getResource("icon.png")
        if (iconUrl != null) {
            val image = javax.imageio.ImageIO.read(iconUrl)
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar.getTaskbar().iconImage = image
            }
        }
    } catch (_: Exception) {
        // Taskbar API not available on this platform
    }

    application {
    val appState = remember { AppState() }
    val timerViewModel = remember {
        TimerViewModel(appState.timewCli) { appState.showError(it) }
    }
    val timelineViewModel = remember {
        TimelineViewModel(appState.timewCli) { appState.showError(it) }
    }
    val tagViewModel = remember { TagViewModel(appState.timewCli) }
    val taskRepository = remember { TaskRepository() }
    val taskViewModel = remember {
        TaskViewModel(taskRepository, appState.timewCli) { appState.showError(it) }
    }
    val overtimeViewModel = remember {
        OvertimeViewModel(appState.timewCli, appState) { appState.showError(it) }
    }
    val idleViewModel = remember {
        IdleViewModel(appState.timewCli, timerViewModel, appState) { appState.showError(it) }
    }

    var showStartTimerDialog by remember { mutableStateOf(false) }
    var isWindowVisible by remember { mutableStateOf(true) }
    var recentTagSets by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    val appIcon = remember {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
        stream?.use {
            BitmapPainter(org.jetbrains.skia.Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap())
        }
    }

    // Refresh overtime balance when timer stops
    LaunchedEffect(timerViewModel.isRunning) {
        if (!timerViewModel.isRunning) {
            overtimeViewModel.refresh()
        }
    }

    // Auto-sync absence.io on launch when enabled
    LaunchedEffect(Unit) {
        if (appState.absenceIoEnabled) {
            overtimeViewModel.syncAbsences()
        }
    }

    // Fetch recent tag combinations for tray quick-start menu
    LaunchedEffect(timerViewModel.isRunning) {
        appState.timewCli.exportIntervals(":week")
            .onSuccess { intervals ->
                recentTagSets = intervals
                    .map { it.tags.sorted() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(5)
            }
    }

    // System tray — lets user control timer without opening the window
    appIcon?.let { icon ->
        Tray(
            icon = icon,
            tooltip = if (timerViewModel.isRunning) {
                "TimewGUI \u2014 ${formatElapsed(timerViewModel.elapsedTime)}"
            } else {
                "TimewGUI"
            },
            onAction = { isWindowVisible = true },
            menu = {
                if (timerViewModel.isRunning) {
                    Item(
                        "\u25CF Tracking: ${formatElapsed(timerViewModel.elapsedTime)}",
                        enabled = false,
                        onClick = {}
                    )
                    timerViewModel.activeInterval?.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                        Item(
                            "   ${tags.joinToString(", ")}",
                            enabled = false,
                            onClick = {}
                        )
                    }
                    Separator()
                    Item("Stop Timer") { timerViewModel.stopTimer() }
                } else {
                    Item("\u25CB Idle", enabled = false, onClick = {})
                    Separator()
                    Item("Start Timer\u2026") {
                        isWindowVisible = true
                        showStartTimerDialog = true
                    }
                }

                if (recentTagSets.isNotEmpty()) {
                    Separator()
                    Menu("Quick Start") {
                        recentTagSets.forEach { tags ->
                            Item(tags.joinToString(", ")) {
                                timerViewModel.startTimer(tags)
                            }
                        }
                    }
                }

                Separator()
                if (!isWindowVisible) {
                    Item("Show Window") { isWindowVisible = true }
                }
                Item("Quit TimewGUI") { exitApplication() }
            }
        )
    }

    Window(
        visible = isWindowVisible,
        onCloseRequest = { isWindowVisible = false },
        title = "TimewGUI",
        icon = appIcon,
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        TimewGuiTheme(darkTheme = appState.isDarkTheme ?: isSystemInDarkTheme()) {
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(appState.errorMessage) {
                appState.errorMessage?.let { message ->
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Dismiss"
                    )
                    appState.clearError()
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    currentScreen = appState.currentScreen,
                    expanded = appState.sidebarExpanded,
                    onScreenSelected = { appState.navigateTo(it) },
                    onToggleExpanded = { appState.toggleSidebar() }
                )
                Column(modifier = Modifier.weight(1f)) {
                    TopBar(
                        isTimerRunning = timerViewModel.isRunning,
                        elapsedTime = formatElapsed(timerViewModel.elapsedTime),
                        activeTags = timerViewModel.activeInterval?.tags ?: emptyList(),
                        onStartClick = { showStartTimerDialog = true },
                        onStopClick = { timerViewModel.stopTimer() }
                    )
                    when (appState.currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            timerViewModel = timerViewModel,
                            timelineViewModel = timelineViewModel,
                            tagViewModel = tagViewModel,
                            overtimeViewModel = overtimeViewModel,
                            appState = appState,
                            onStartTimer = { showStartTimerDialog = true },
                            onContinueInterval = { interval ->
                                timerViewModel.continueTimer(interval.id)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Screen.TIMELINE -> TimelineScreen(
                            timelineViewModel = timelineViewModel,
                            tagViewModel = tagViewModel,
                            modifier = Modifier.weight(1f)
                        )
                        Screen.REPORTS -> ReportsScreen(
                            timelineViewModel = timelineViewModel,
                            tagViewModel = tagViewModel,
                            modifier = Modifier.weight(1f)
                        )
                        Screen.TAGS -> TagsScreen(
                            tagViewModel = tagViewModel,
                            timelineViewModel = timelineViewModel,
                            modifier = Modifier.weight(1f)
                        )
                        Screen.TASKS -> TasksScreen(
                            taskViewModel = taskViewModel,
                            timerViewModel = timerViewModel,
                            tagViewModel = tagViewModel,
                            appState = appState,
                            modifier = Modifier.weight(1f)
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            appState = appState,
                            tagViewModel = tagViewModel,
                            overtimeViewModel = overtimeViewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (showStartTimerDialog) {
                StartTimerDialog(
                    availableTags = tagViewModel.availableTags,
                    onStart = { tags ->
                        timerViewModel.startTimer(tags)
                        showStartTimerDialog = false
                    },
                    onDismiss = { showStartTimerDialog = false }
                )
            }

            if (idleViewModel.isIdleDialogShowing) {
                IdleDialog(
                    idleDurationMinutes = idleViewModel.idleDurationMinutes,
                    onKeepTracking = { idleViewModel.keepTracking() },
                    onPauseAndResume = { idleViewModel.pauseAndResume() },
                    onStopTimer = { idleViewModel.discardIdleTime() }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
            )
        }
    }
    }
}

private fun formatElapsed(duration: kotlin.time.Duration): String {
    val totalMinutes = duration.inWholeMinutes
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}
