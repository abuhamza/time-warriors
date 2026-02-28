package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.idle.IdleDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

class IdleViewModel(
    private val timewCli: TimewCli,
    private val timerViewModel: TimerViewModel,
    private val appState: AppState,
    private val onError: (String) -> Unit = {}
) {
    private val idleDetector = IdleDetector()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var pollJob: Job? = null

    var isIdleDialogShowing: Boolean by mutableStateOf(false)
        private set

    var idleDurationMinutes: Long by mutableStateOf(0)
        private set

    private var idleStartEpochMs: Long = 0L
    private var idleTags: List<String> = emptyList()

    init {
        scope.launch {
            while (isActive) {
                if (timerViewModel.isRunning && appState.idleDetectionEnabled && !isIdleDialogShowing) {
                    val idleSecs = idleDetector.getIdleSeconds()
                    val thresholdSecs = appState.idleThresholdMinutes * 60L

                    if (idleSecs >= thresholdSecs) {
                        idleStartEpochMs = System.currentTimeMillis() - (idleSecs * 1000)
                        idleDurationMinutes = idleSecs / 60
                        idleTags = timerViewModel.activeInterval?.tags ?: emptyList()
                        isIdleDialogShowing = true
                    }
                }
                delay(30.seconds)
            }
        }
    }

    fun keepTracking() {
        isIdleDialogShowing = false
    }

    fun pauseAndResume() {
        scope.launch {
            val tags = idleTags
            val idleTime = formatEpochForTimew(idleStartEpochMs)

            timewCli.stop()
                .onSuccess {
                    timewCli.modifyEnd(1, idleTime)
                        .onFailure { e -> onError("Failed to adjust interval: ${e.message}") }
                    timewCli.start(tags = tags)
                        .onFailure { e -> onError("Failed to restart timer: ${e.message}") }
                    timerViewModel.refreshActiveState()
                }
                .onFailure { e -> onError("Failed to stop timer: ${e.message}") }

            isIdleDialogShowing = false
        }
    }

    fun discardIdleTime() {
        scope.launch {
            val idleTime = formatEpochForTimew(idleStartEpochMs)

            timewCli.stop()
                .onSuccess {
                    timewCli.modifyEnd(1, idleTime)
                        .onFailure { e -> onError("Failed to adjust interval: ${e.message}") }
                    timerViewModel.refreshActiveState()
                }
                .onFailure { e -> onError("Failed to stop timer: ${e.message}") }

            isIdleDialogShowing = false
        }
    }

    fun cancel() {
        pollJob?.cancel()
        job.cancel()
    }

    private fun formatEpochForTimew(epochMs: Long): String {
        val instant = java.time.Instant.ofEpochMilli(epochMs)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
