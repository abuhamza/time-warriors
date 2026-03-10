package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.timewgui.domain.RecurrenceEngine
import com.timewgui.domain.api.AiToolsClient
import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.model.GeneratedTask
import com.timewgui.domain.model.RecurrenceRule
import com.timewgui.domain.model.Task
import com.timewgui.domain.model.TaskStatus
import com.timewgui.domain.repository.TaskRepository
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Duration

class TaskViewModel(
    private val taskRepository: TaskRepository,
    private val timewCli: TimewCli,
    private val aiToolsClient: AiToolsClient? = null,
    private val onError: (String) -> Unit = {}
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var tasks: List<Task> by mutableStateOf(emptyList())
        private set
    var timePerTask: Map<String, Duration> by mutableStateOf(emptyMap())
        private set
    var isLoadingTime: Boolean by mutableStateOf(false)
        private set
    var generatedTasks: List<GeneratedTask> by mutableStateOf(emptyList())
        private set
    var isGenerating: Boolean by mutableStateOf(false)
        private set
    var generationError: String? by mutableStateOf(null)
        private set

    init {
        loadTasks()
        generateInstancesForToday()
    }

    private fun loadTasks() {
        tasks = taskRepository.load()
    }

    fun createRecurringTask(title: String, contextTags: List<String>, rule: RecurrenceRule) {
        val tag = taskRepository.generateTag(title)
        val template = Task(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            tag = tag,
            contextTags = contextTags,
            status = TaskStatus.TODO,
            createdAt = System.currentTimeMillis(),
            sortOrder = tasks.size,
            recurrenceRule = rule,
        )
        tasks = tasks + template
        saveTasks()
        generateInstancesForToday()
    }

    fun generateInstancesForToday() {
        val today = LocalDate.now()
        val todayStart = RecurrenceEngine.startOfDay(today)
        val templates = tasks.filter { it.recurrenceRule != null }
        var current = tasks
        var changed = false
        for (template in templates) {
            val existingToday = current.any {
                it.recurrenceTemplateId == template.id && it.scheduledDate == todayStart
            }
            if (existingToday) continue
            val generatedCount = current.count { it.recurrenceTemplateId == template.id }
            if (RecurrenceEngine.shouldGenerateForDate(template, today, generatedCount)) {
                current = current + RecurrenceEngine.generateInstance(template, today)
                changed = true
            }
        }
        if (changed) {
            tasks = current
            saveTasks()
        }
    }

    fun deleteTemplate(templateId: String) {
        val todayStart = RecurrenceEngine.startOfDay(LocalDate.now())
        tasks = tasks.filter { task ->
            when {
                task.id == templateId -> false
                task.recurrenceTemplateId == templateId ->
                    task.scheduledDate != null && task.scheduledDate < todayStart
                else -> true
            }
        }
        saveTasks()
    }

    fun updateRecurrenceRule(templateId: String, rule: RecurrenceRule?) {
        tasks = tasks.map { if (it.id == templateId) it.copy(recurrenceRule = rule) else it }
        saveTasks()
        if (rule != null) generateInstancesForToday()
    }

    fun createTask(title: String, contextTags: List<String>) {
        val tag = taskRepository.generateTag(title)
        val task = Task(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            tag = tag,
            contextTags = contextTags,
            status = TaskStatus.TODO,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            sortOrder = tasks.size
        )
        tasks = tasks + task
        saveTasks()
    }

    fun updateTask(task: Task) {
        tasks = tasks.map { if (it.id == task.id) task else it }
        saveTasks()
    }

    fun updateStatus(taskId: String, status: TaskStatus) {
        tasks = tasks.map { task ->
            if (task.id == taskId) {
                when (status) {
                    TaskStatus.DONE -> task.copy(
                        status = status,
                        completedAt = System.currentTimeMillis()
                    )
                    TaskStatus.IN_PROGRESS -> task.copy(
                        status = status,
                        completedAt = null
                    )
                    else -> task.copy(status = status)
                }
            } else {
                task
            }
        }
        saveTasks()
    }

    fun deleteTask(taskId: String) {
        tasks = tasks.filter { it.id != taskId }
        saveTasks()
    }

    fun startTimerForTask(task: Task, timerViewModel: TimerViewModel) {
        val allTags = task.contextTags + task.tag
        if (task.status == TaskStatus.TODO) {
            updateStatus(task.id, TaskStatus.IN_PROGRESS)
        }
        scope.launch {
            timewCli.start(tags = allTags)
                .onSuccess {
                    timerViewModel.refreshActiveState()
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to start timer for task")
                }
        }
    }

    fun refreshTimeAggregation() {
        isLoadingTime = true
        scope.launch {
            timewCli.exportIntervals()
                .onSuccess { intervals ->
                    val taskTags = tasks.associate { it.tag to it.id }
                    val aggregated = mutableMapOf<String, Duration>()
                    for (interval in intervals) {
                        for (intervalTag in interval.tags) {
                            val matchedTag = taskTags.keys.find { it == intervalTag }
                            if (matchedTag != null) {
                                aggregated[matchedTag] = (aggregated[matchedTag] ?: Duration.ZERO) + interval.duration
                            }
                        }
                    }
                    timePerTask = aggregated
                    isLoadingTime = false
                }
                .onFailure { e ->
                    onError(e.message ?: "Failed to load time aggregation")
                    isLoadingTime = false
                }
        }
    }

    fun generateTasksFromBrainDump(text: String) {
        val client = aiToolsClient ?: run {
            generationError = "API not configured. Set API URL and token in Settings."
            return
        }
        isGenerating = true
        generationError = null
        generatedTasks = emptyList()
        scope.launch {
            val existingTags = tasks.flatMap { it.contextTags }.distinct()
            client.generateTasks(text, existingTags)
                .onSuccess { result ->
                    generatedTasks = result
                    isGenerating = false
                }
                .onFailure { e ->
                    generationError = e.message ?: "Failed to generate tasks"
                    isGenerating = false
                }
        }
    }

    fun createTasksFromGenerated(selectedTasks: List<GeneratedTask>) {
        for (generated in selectedTasks) {
            val tag = taskRepository.generateTag(generated.title)
            val task = Task(
                id = java.util.UUID.randomUUID().toString(),
                title = generated.title,
                tag = tag,
                contextTags = generated.tags,
                status = TaskStatus.TODO,
                createdAt = System.currentTimeMillis(),
                sortOrder = tasks.size
            )
            tasks = tasks + task
        }
        saveTasks()
        clearBrainDump()
    }

    fun clearBrainDump() {
        generatedTasks = emptyList()
        generationError = null
        isGenerating = false
    }

    private fun saveTasks() {
        taskRepository.save(tasks)
    }
}
