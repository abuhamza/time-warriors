package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.model.Interval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimerViewModel(
    private val timewCli: TimewCli,
    private val onError: (String) -> Unit = {}
) {
    var activeInterval: Interval? by mutableStateOf(null)
        private set

    var elapsedTime: Duration by mutableStateOf(Duration.ZERO)
        private set

    val isRunning: Boolean
        get() = activeInterval != null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main.immediate)
    private var tickJob: Job? = null

    init {
        scope.launch {
            refreshActiveState()
            startTickingIfActive()
        }
    }

    fun startTimer(tags: List<String> = emptyList(), startTime: String? = null) {
        scope.launch {
            timewCli.start(tags = tags, startTime = startTime)
                .onSuccess {
                    refreshActiveState()
                    startTickingIfActive()
                }
                .onFailure { e -> onError(e.message ?: "Failed to start timer") }
        }
    }

    fun stopTimer() {
        scope.launch {
            timewCli.stop()
                .onSuccess {
                    stopTicking()
                    refreshActiveState()
                }
                .onFailure { e -> onError(e.message ?: "Failed to stop timer") }
        }
    }

    fun cancelTimer() {
        scope.launch {
            timewCli.cancel()
                .onSuccess {
                    stopTicking()
                    refreshActiveState()
                }
                .onFailure { e -> onError(e.message ?: "Failed to cancel timer") }
        }
    }

    fun continueTimer(id: Int? = null) {
        scope.launch {
            timewCli.continueTracking(id = id)
                .onSuccess {
                    refreshActiveState()
                    startTickingIfActive()
                }
                .onFailure { e -> onError(e.message ?: "Failed to continue timer") }
        }
    }

    fun refreshActiveState() {
        scope.launch {
            timewCli.getActiveInterval()
                .onSuccess { interval ->
                    activeInterval = interval
                    if (interval != null) {
                        updateElapsedTime(interval)
                    } else {
                        elapsedTime = Duration.ZERO
                    }
                }
                .onFailure { e -> onError(e.message ?: "Failed to get active state") }
        }
    }

    fun cancel() {
        stopTicking()
        job.cancel()
    }

    private fun startTickingIfActive() {
        if (activeInterval != null && tickJob?.isActive != true) {
            tickJob = scope.launch {
                while (scope.isActive && activeInterval != null) {
                    activeInterval?.let { updateElapsedTime(it) }
                    delay(1.seconds)
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    private fun updateElapsedTime(interval: Interval) {
        val now = kotlin.time.Clock.System.now()
        val ms = now.toEpochMilliseconds() - interval.start.toEpochMilliseconds()
        elapsedTime = ms.milliseconds
    }
}
