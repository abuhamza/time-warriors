package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.timewgui.domain.model.Task
import com.timewgui.domain.model.TaskStatus
import com.timewgui.ui.components.TagSelector
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.ui.theme.TimewTypography
import com.timewgui.viewmodel.AppState
import com.timewgui.viewmodel.TagViewModel
import com.timewgui.viewmodel.TaskViewModel
import com.timewgui.viewmodel.TimerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TasksScreen(
    taskViewModel: TaskViewModel,
    timerViewModel: TimerViewModel,
    tagViewModel: TagViewModel,
    appState: AppState,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current

    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var newTaskTitle by remember { mutableStateOf("") }

    val screenFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { screenFocusRequester.requestFocus() }
    LaunchedEffect(showCreateForm) {
        if (showCreateForm) titleFocusRequester.requestFocus()
        else screenFocusRequester.requestFocus()
    }

    val filteredTasks = remember(taskViewModel.tasks, statusFilter) {
        when (statusFilter) {
            null -> taskViewModel.tasks.filter { it.status != TaskStatus.ARCHIVED }
            else -> taskViewModel.tasks.filter { it.status == statusFilter }
        }
    }

    val selectedTask = remember(selectedTaskId, taskViewModel.tasks) {
        taskViewModel.tasks.find { it.id == selectedTaskId }
    }

    LaunchedEffect(Unit) {
        taskViewModel.refreshTimeAggregation()
    }

    // Re-fetch time aggregation when the timer starts or stops
    LaunchedEffect(timerViewModel.isRunning) {
        taskViewModel.refreshTimeAggregation()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgPrimary)
            .padding(TimewDimensions.sectionGap)
            .focusRequester(screenFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (!showCreateForm && keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.N) {
                    showCreateForm = true
                    true
                } else false
            }
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Button(
                onClick = { showCreateForm = true },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ New Task")
            }
        }

        Spacer(modifier = Modifier.height(TimewDimensions.sectionGap))

        // Create form
        if (showCreateForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                    .background(colors.cardSurface)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Task title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter -> {
                                    if (newTaskTitle.isNotBlank()) {
                                        taskViewModel.createTask(newTaskTitle, appState.defaultContextTags)
                                        newTaskTitle = ""
                                        showCreateForm = false
                                    }
                                    true
                                }
                                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape -> {
                                    newTaskTitle = ""
                                    showCreateForm = false
                                    true
                                }
                                else -> false
                            }
                        },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        focusedLabelColor = colors.accent,
                        unfocusedLabelColor = colors.textOnCardSecondary,
                        cursorColor = colors.accent,
                        focusedTextColor = colors.textOnCardPrimary,
                        unfocusedTextColor = colors.textOnCardPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                taskViewModel.createTask(newTaskTitle, appState.defaultContextTags)
                                newTaskTitle = ""
                                showCreateForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create")
                    }
                    Button(
                        onClick = {
                            newTaskTitle = ""
                            showCreateForm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgTertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = colors.textPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(TimewDimensions.sectionGap))
        }

        // Filter chips row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data class FilterOption(val label: String, val status: TaskStatus?)
            val filterOptions = listOf(
                FilterOption("All", null),
                FilterOption("Todo", TaskStatus.TODO),
                FilterOption("Active", TaskStatus.IN_PROGRESS),
                FilterOption("Done", TaskStatus.DONE)
            )
            filterOptions.forEach { option ->
                val isSelected = statusFilter == option.status
                Button(
                    onClick = { statusFilter = option.status },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) colors.accent else colors.bgTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = option.label,
                        color = if (isSelected) Color.White else colors.textPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(TimewDimensions.sectionGap))

        // Two-pane layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
        ) {
            // LEFT PANE: Task list
            Column(modifier = Modifier.weight(1f)) {
                val showDateGroups = statusFilter == TaskStatus.DONE || statusFilter == null

                // For All tab: split into non-done (flat) + done (date-grouped)
                val nonDoneTasks = remember(filteredTasks, statusFilter) {
                    if (statusFilter == null) filteredTasks.filter { it.status != TaskStatus.DONE }
                    else emptyList()
                }
                val doneTasks = remember(filteredTasks, showDateGroups) {
                    if (showDateGroups) {
                        val tasks = if (statusFilter == null) {
                            filteredTasks.filter { it.status == TaskStatus.DONE }
                        } else {
                            filteredTasks
                        }
                        groupTasksByCompletionDate(tasks)
                    } else {
                        null
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Non-done tasks (only on All tab, shown first as flat list)
                    if (nonDoneTasks.isNotEmpty()) {
                        items(nonDoneTasks, key = { it.id }) { task ->
                            TaskListItem(
                                task = task,
                                isSelected = task.id == selectedTaskId,
                                taskViewModel = taskViewModel,
                                colors = colors,
                                onClick = {
                                    selectedTaskId = if (task.id == selectedTaskId) null else task.id
                                }
                            )
                        }
                    }

                    // Date-grouped done tasks (All tab and Done tab)
                    if (doneTasks != null) {
                        doneTasks.forEach { (dateLabel, tasksInGroup) ->
                            item(key = "header-$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(tasksInGroup, key = { it.id }) { task ->
                                TaskListItem(
                                    task = task,
                                    isSelected = task.id == selectedTaskId,
                                    taskViewModel = taskViewModel,
                                    colors = colors,
                                    onClick = {
                                        selectedTaskId = if (task.id == selectedTaskId) null else task.id
                                    }
                                )
                            }
                        }
                    }

                    // Flat list for Todo and Active tabs
                    if (doneTasks == null && nonDoneTasks.isEmpty()) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskListItem(
                                task = task,
                                isSelected = task.id == selectedTaskId,
                                taskViewModel = taskViewModel,
                                colors = colors,
                                onClick = {
                                    selectedTaskId = if (task.id == selectedTaskId) null else task.id
                                }
                            )
                        }
                    }
                }
            }

            // RIGHT PANE: Detail/Edit form or empty state
            if (selectedTask != null) {
                Column(modifier = Modifier.weight(1f)) {
                    var editTitle by remember(selectedTask.id) { mutableStateOf(selectedTask.title) }
                    var editNote by remember(selectedTask.id) { mutableStateOf(selectedTask.note) }
                    var editStatus by remember(selectedTask.id) { mutableStateOf(selectedTask.status) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                            .background(colors.cardSurface)
                            .padding(16.dp)
                    ) {
                        // Close button row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { selectedTaskId = null },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close detail panel",
                                    tint = colors.textOnCardSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Title field
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { newTitle ->
                                editTitle = newTitle
                                taskViewModel.updateTask(selectedTask.copy(title = newTitle))
                            },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.borderOnCard,
                                focusedLabelColor = colors.accent,
                                unfocusedLabelColor = colors.textOnCardSecondary,
                                cursorColor = colors.accent,
                                focusedTextColor = colors.textOnCardPrimary,
                                unfocusedTextColor = colors.textOnCardPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tag display
                        Text(
                            text = "Tag: ${selectedTask.tag}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textOnCardSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Context tags
                        TagSelector(
                            selectedTags = selectedTask.contextTags,
                            availableTags = tagViewModel.availableTags,
                            onTagAdded = { tag ->
                                val updated = selectedTask.copy(contextTags = selectedTask.contextTags + tag)
                                taskViewModel.updateTask(updated)
                            },
                            onTagRemoved = { tag ->
                                val updated = selectedTask.copy(contextTags = selectedTask.contextTags - tag)
                                taskViewModel.updateTask(updated)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status selector
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textOnCardSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TaskStatus.entries.forEach { status ->
                                val isActive = editStatus == status
                                Button(
                                    onClick = {
                                        editStatus = status
                                        taskViewModel.updateStatus(selectedTask.id, status)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) statusColor(status, colors) else colors.bgTertiary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = status.name.replace("_", " "),
                                        color = if (isActive) Color.White else colors.textPrimary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Note field
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { newNote ->
                                editNote = newNote
                                taskViewModel.updateTask(selectedTask.copy(note = newNote))
                            },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.borderOnCard,
                                focusedLabelColor = colors.accent,
                                unfocusedLabelColor = colors.textOnCardSecondary,
                                cursorColor = colors.accent,
                                focusedTextColor = colors.textOnCardPrimary,
                                unfocusedTextColor = colors.textOnCardPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    taskViewModel.startTimerForTask(selectedTask, timerViewModel)
                                },
                                enabled = selectedTask.status != TaskStatus.DONE &&
                                        selectedTask.status != TaskStatus.ARCHIVED,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Start Timer")
                            }
                            Button(
                                onClick = {
                                    taskViewModel.deleteTask(selectedTask.id)
                                    selectedTaskId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.destructive),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    isSelected: Boolean,
    taskViewModel: TaskViewModel,
    colors: com.timewgui.ui.theme.TimewColors,
    onClick: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) lerp(colors.cardSurface, colors.accent, 0.15f)
                else colors.cardSurface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isDone,
            onCheckedChange = { checked ->
                if (checked) {
                    taskViewModel.updateStatus(task.id, TaskStatus.DONE)
                } else {
                    taskViewModel.updateStatus(task.id, TaskStatus.TODO)
                }
            },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.success,
                uncheckedColor = colors.textOnCardPrimary,
                checkmarkColor = Color.White
            ),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        if (task.status == TaskStatus.IN_PROGRESS) {
            Text(
                text = "\u25B6",
                color = colors.textOnCardSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isDone) FontWeight.Normal else FontWeight.Medium,
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (isDone) colors.textOnCardTertiary else colors.textOnCardPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        val duration = taskViewModel.timePerTask[task.tag]
        if (duration != null) {
            Text(
                text = formatTaskDuration(duration),
                style = TimewTypography.monospace,
                color = colors.textOnCardSecondary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun groupTasksByCompletionDate(tasks: List<Task>): List<Pair<String, List<Task>>> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val yesterday = today.minusDays(1)
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    return tasks
        .sortedByDescending { it.completedAt ?: 0L }
        .groupBy { task ->
            val completedAt = task.completedAt
            if (completedAt != null) {
                Instant.ofEpochMilli(completedAt).atZone(zone).toLocalDate()
            } else {
                null
            }
        }
        .map { (date, tasksInGroup) ->
            val label = when (date) {
                today -> "Today"
                yesterday -> "Yesterday"
                null -> "Unknown"
                else -> date.format(dateFormatter)
            }
            label to tasksInGroup
        }
}

@Composable
private fun statusColor(
    status: TaskStatus,
    colors: com.timewgui.ui.theme.TimewColors
): Color = when (status) {
    TaskStatus.TODO -> colors.textSecondary
    TaskStatus.IN_PROGRESS -> colors.accent
    TaskStatus.DONE -> colors.success
    TaskStatus.ARCHIVED -> colors.textTertiary
}

private fun formatTaskDuration(d: kotlin.time.Duration): String {
    val totalMinutes = d.inWholeMinutes
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
}
