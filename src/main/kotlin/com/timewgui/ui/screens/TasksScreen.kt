package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.timewgui.domain.model.GeneratedTask
import com.timewgui.domain.model.RecurrenceEndType
import com.timewgui.domain.model.RecurrenceFrequency
import com.timewgui.domain.model.RecurrenceRule
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

private enum class TaskTab { ALL, TODO, ACTIVE, DONE, RECURRING }

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
    var activeTab by remember { mutableStateOf(TaskTab.ALL) }
    var newTaskTitle by remember { mutableStateOf("") }
    var showBrainDump by remember { mutableStateOf(false) }
    var brainDumpText by remember { mutableStateOf("") }

    // Recurring create-form state
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.DAILY) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var selectedEndType by remember { mutableStateOf(RecurrenceEndType.FOREVER) }
    var endDateText by remember { mutableStateOf("") }
    var maxOccurrencesText by remember { mutableStateOf("") }

    val screenFocusRequester = remember { FocusRequester() }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { screenFocusRequester.requestFocus() }
    LaunchedEffect(showCreateForm) {
        if (showCreateForm) titleFocusRequester.requestFocus()
        else screenFocusRequester.requestFocus()
    }

    fun resetForm() {
        newTaskTitle = ""
        isRecurring = false
        selectedFrequency = RecurrenceFrequency.DAILY
        selectedDays = emptySet()
        selectedEndType = RecurrenceEndType.FOREVER
        endDateText = ""
        maxOccurrencesText = ""
        showCreateForm = false
    }

    fun submitCreate() {
        if (newTaskTitle.isBlank()) return
        if (isRecurring) {
            val endDate = if (selectedEndType == RecurrenceEndType.UNTIL_DATE) {
                parseDateToEpoch(endDateText)
            } else null
            val maxOccurrences = if (selectedEndType == RecurrenceEndType.AFTER_OCCURRENCES) {
                maxOccurrencesText.toIntOrNull()
            } else null
            val rule = RecurrenceRule(
                frequency = selectedFrequency,
                daysOfWeek = selectedDays,
                endType = selectedEndType,
                endDate = endDate,
                maxOccurrences = maxOccurrences,
            )
            taskViewModel.createRecurringTask(newTaskTitle, appState.defaultContextTags, rule)
        } else {
            taskViewModel.createTask(newTaskTitle, appState.defaultContextTags)
        }
        resetForm()
    }

    val filteredTasks = remember(taskViewModel.tasks, activeTab) {
        when (activeTab) {
            TaskTab.ALL -> taskViewModel.tasks.filter {
                it.status != TaskStatus.ARCHIVED && it.recurrenceRule == null
            }
            TaskTab.TODO -> taskViewModel.tasks.filter {
                it.status == TaskStatus.TODO && it.recurrenceRule == null
            }
            TaskTab.ACTIVE -> taskViewModel.tasks.filter {
                it.status == TaskStatus.IN_PROGRESS && it.recurrenceRule == null
            }
            TaskTab.DONE -> taskViewModel.tasks.filter {
                it.status == TaskStatus.DONE && it.recurrenceRule == null
            }
            TaskTab.RECURRING -> taskViewModel.tasks.filter { it.recurrenceRule != null }
        }
    }

    val selectedTask = remember(selectedTaskId, taskViewModel.tasks) {
        taskViewModel.tasks.find { it.id == selectedTaskId }
    }

    LaunchedEffect(Unit) { taskViewModel.refreshTimeAggregation() }
    LaunchedEffect(timerViewModel.isRunning) { taskViewModel.refreshTimeAggregation() }

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showBrainDump = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.bgPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Plan Todo",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plan Todo")
                }
                Button(
                    onClick = { showCreateForm = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.bgPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ New Task")
                }
            }
        }

        Spacer(modifier = Modifier.height(TimewDimensions.sectionGap))

        // Brain dump input section
        if (showBrainDump && taskViewModel.generatedTasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                    .background(colors.cardSurface)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = brainDumpText,
                    onValueChange = { brainDumpText = it },
                    placeholder = { Text("What's on your mind? Describe your day, tasks, appointments...", color = colors.textOnCardTertiary) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    enabled = !taskViewModel.isGenerating,
                    maxLines = 6,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.borderOnCard,
                        cursorColor = colors.accent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { taskViewModel.generateTasksFromBrainDump(brainDumpText) },
                        enabled = brainDumpText.isNotBlank() && !taskViewModel.isGenerating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.bgPrimary
                        )
                    ) {
                        if (taskViewModel.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.bgPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (taskViewModel.isGenerating) "Generating..." else "Generate")
                    }
                    TextButton(onClick = {
                        showBrainDump = false
                        brainDumpText = ""
                        taskViewModel.clearBrainDump()
                    }) {
                        Text("Cancel", color = colors.textOnCardSecondary)
                    }
                }
                taskViewModel.generationError?.let { error ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error, color = colors.destructive, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Generated tasks preview/confirmation section
        if (taskViewModel.generatedTasks.isNotEmpty()) {
            val selectedIndices = remember(taskViewModel.generatedTasks) {
                mutableStateListOf<Int>().apply { addAll(taskViewModel.generatedTasks.indices) }
            }
            val editedTasks = remember(taskViewModel.generatedTasks) {
                mutableStateListOf<GeneratedTask>().apply { addAll(taskViewModel.generatedTasks) }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                    .background(colors.cardSurface)
                    .padding(16.dp)
            ) {
                Text("Generated Tasks", style = MaterialTheme.typography.titleSmall, color = colors.textOnCardPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                editedTasks.forEachIndexed { index, task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = index in selectedIndices,
                            onCheckedChange = { checked ->
                                if (checked) selectedIndices.add(index) else selectedIndices.removeAll { it == index }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                uncheckedColor = colors.textOnCardSecondary
                            )
                        )
                        OutlinedTextField(
                            value = task.title,
                            onValueChange = { newTitle -> editedTasks[index] = task.copy(title = newTitle) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textOnCardPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.borderOnCard,
                                cursorColor = colors.accent
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        task.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(tag, style = MaterialTheme.typography.labelSmall, color = colors.accent)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val selected = selectedIndices.map { editedTasks[it] }
                            taskViewModel.createTasksFromGenerated(selected)
                            showBrainDump = false
                            brainDumpText = ""
                        },
                        enabled = selectedIndices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.bgPrimary
                        )
                    ) {
                        Text("Create ${selectedIndices.size} Task${if (selectedIndices.size != 1) "s" else ""}")
                    }
                    TextButton(onClick = {
                        taskViewModel.clearBrainDump()
                        showBrainDump = false
                        brainDumpText = ""
                    }) {
                        Text("Cancel", color = colors.textOnCardSecondary)
                    }
                }
            }
        }

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
                                    submitCreate(); true
                                }
                                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape -> {
                                    resetForm(); true
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

                // Recurring toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isRecurring = !isRecurring }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RadioButton(
                        selected = isRecurring,
                        onClick = { isRecurring = !isRecurring },
                        colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                    )
                    Text(
                        "Repeating task",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textOnCardPrimary
                    )
                }

                if (isRecurring) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Frequency",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textOnCardSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RecurrenceFrequency.entries.forEach { freq ->
                            val isSelected = selectedFrequency == freq
                            Button(
                                onClick = { selectedFrequency = freq },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) colors.accent else colors.bgTertiary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    freq.name.lowercase().replaceFirstChar { it.uppercase() },
                                    color = if (isSelected) Color.White else colors.textPrimary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (selectedFrequency == RecurrenceFrequency.WEEKLY) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Days",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textOnCardSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1 to "M", 2 to "T", 3 to "W", 4 to "Th", 5 to "F", 6 to "Sa", 7 to "Su")
                                .forEach { (day, label) ->
                                    val isSelected = day in selectedDays
                                    Button(
                                        onClick = {
                                            selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) colors.accent else colors.bgTertiary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            label,
                                            color = if (isSelected) Color.White else colors.textPrimary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ends",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textOnCardSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedEndType = RecurrenceEndType.FOREVER }
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedEndType == RecurrenceEndType.FOREVER,
                                onClick = { selectedEndType = RecurrenceEndType.FOREVER },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                            )
                            Text("Never", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedEndType = RecurrenceEndType.UNTIL_DATE }
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RadioButton(
                                selected = selectedEndType == RecurrenceEndType.UNTIL_DATE,
                                onClick = { selectedEndType = RecurrenceEndType.UNTIL_DATE },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                            )
                            Text("On date:", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                            if (selectedEndType == RecurrenceEndType.UNTIL_DATE) {
                                OutlinedTextField(
                                    value = endDateText,
                                    onValueChange = { endDateText = it },
                                    placeholder = { Text("yyyy-MM-dd", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.width(140.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.accent,
                                        unfocusedBorderColor = colors.borderOnCard,
                                        cursorColor = colors.accent,
                                        focusedTextColor = colors.textOnCardPrimary,
                                        unfocusedTextColor = colors.textOnCardPrimary
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedEndType = RecurrenceEndType.AFTER_OCCURRENCES }
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RadioButton(
                                selected = selectedEndType == RecurrenceEndType.AFTER_OCCURRENCES,
                                onClick = { selectedEndType = RecurrenceEndType.AFTER_OCCURRENCES },
                                colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                            )
                            Text("After", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                            if (selectedEndType == RecurrenceEndType.AFTER_OCCURRENCES) {
                                OutlinedTextField(
                                    value = maxOccurrencesText,
                                    onValueChange = { maxOccurrencesText = it.filter { c -> c.isDigit() } },
                                    placeholder = { Text("N", style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.width(72.dp),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.accent,
                                        unfocusedBorderColor = colors.borderOnCard,
                                        cursorColor = colors.accent,
                                        focusedTextColor = colors.textOnCardPrimary,
                                        unfocusedTextColor = colors.textOnCardPrimary
                                    )
                                )
                                Text("occurrences", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { submitCreate() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create")
                    }
                    Button(
                        onClick = { resetForm() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgTertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = colors.textPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(TimewDimensions.sectionGap))
        }

        // Filter tabs row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            data class TabOption(val label: String, val tab: TaskTab)
            listOf(
                TabOption("All", TaskTab.ALL),
                TabOption("Todo", TaskTab.TODO),
                TabOption("Active", TaskTab.ACTIVE),
                TabOption("Done", TaskTab.DONE),
                TabOption("Recurring", TaskTab.RECURRING),
            ).forEach { option ->
                val isSelected = activeTab == option.tab
                Button(
                    onClick = {
                        activeTab = option.tab
                        selectedTaskId = null
                    },
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
            // LEFT PANE
            Column(modifier = Modifier.weight(1f)) {
                if (activeTab == TaskTab.RECURRING) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredTasks, key = { it.id }) { template ->
                            RecurringTemplateListItem(
                                template = template,
                                isSelected = template.id == selectedTaskId,
                                onClick = {
                                    selectedTaskId = if (template.id == selectedTaskId) null else template.id
                                },
                                onDelete = {
                                    taskViewModel.deleteTemplate(template.id)
                                    if (selectedTaskId == template.id) selectedTaskId = null
                                },
                                colors = colors
                            )
                        }
                    }
                } else {
                    val showDateGroups = activeTab == TaskTab.ALL || activeTab == TaskTab.DONE

                    val nonDoneTasks = remember(filteredTasks, activeTab) {
                        if (activeTab == TaskTab.ALL) filteredTasks.filter { it.status != TaskStatus.DONE }
                        else emptyList()
                    }
                    val doneTasks = remember(filteredTasks, showDateGroups) {
                        if (showDateGroups) {
                            val tasks = if (activeTab == TaskTab.ALL) {
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
            }

            // RIGHT PANE: detail panel
            if (selectedTask != null) {
                Column(modifier = Modifier.weight(1f)) {
                    if (selectedTask.recurrenceRule != null) {
                        TemplateDetailPanel(
                            template = selectedTask,
                            taskViewModel = taskViewModel,
                            onClose = { selectedTaskId = null },
                            colors = colors
                        )
                    } else {
                        TaskDetailPanel(
                            task = selectedTask,
                            taskViewModel = taskViewModel,
                            timerViewModel = timerViewModel,
                            tagViewModel = tagViewModel,
                            onClose = { selectedTaskId = null },
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringTemplateListItem(
    template: Task,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    colors: com.timewgui.ui.theme.TimewColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) lerp(colors.cardSurface, colors.accent, 0.15f) else colors.cardSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "\u21BB",
            color = colors.accent,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 6.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = colors.textOnCardPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val rule = template.recurrenceRule
            if (rule != null) {
                Text(
                    text = ruleDescription(rule),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textOnCardSecondary
                )
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Delete template",
                tint = colors.destructive,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TemplateDetailPanel(
    template: Task,
    taskViewModel: TaskViewModel,
    onClose: () -> Unit,
    colors: com.timewgui.ui.theme.TimewColors
) {
    val rule = template.recurrenceRule ?: return

    var editTitle by remember(template.id) { mutableStateOf(template.title) }
    var editFrequency by remember(template.id) { mutableStateOf(rule.frequency) }
    var editDays by remember(template.id) { mutableStateOf(rule.daysOfWeek) }
    var editEndType by remember(template.id) { mutableStateOf(rule.endType) }
    var editEndDateText by remember(template.id) {
        mutableStateOf(
            rule.endDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            } ?: ""
        )
    }
    var editMaxOccurrencesText by remember(template.id) {
        mutableStateOf(rule.maxOccurrences?.toString() ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
            .background(colors.cardSurface)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close",
                    tint = colors.textOnCardSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        OutlinedTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
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

        Spacer(modifier = Modifier.height(12.dp))

        Text("Frequency", style = MaterialTheme.typography.labelSmall, color = colors.textOnCardSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            RecurrenceFrequency.entries.forEach { freq ->
                val isSelected = editFrequency == freq
                Button(
                    onClick = { editFrequency = freq },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) colors.accent else colors.bgTertiary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        freq.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color.White else colors.textPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (editFrequency == RecurrenceFrequency.WEEKLY) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Days", style = MaterialTheme.typography.labelSmall, color = colors.textOnCardSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1 to "M", 2 to "T", 3 to "W", 4 to "Th", 5 to "F", 6 to "Sa", 7 to "Su")
                    .forEach { (day, label) ->
                        val isSelected = day in editDays
                        Button(
                            onClick = { editDays = if (isSelected) editDays - day else editDays + day },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.accent else colors.bgTertiary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Ends", style = MaterialTheme.typography.labelSmall, color = colors.textOnCardSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .clickable { editEndType = RecurrenceEndType.FOREVER }.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = editEndType == RecurrenceEndType.FOREVER,
                    onClick = { editEndType = RecurrenceEndType.FOREVER },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                )
                Text("Never", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
            }
            Row(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .clickable { editEndType = RecurrenceEndType.UNTIL_DATE }.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(
                    selected = editEndType == RecurrenceEndType.UNTIL_DATE,
                    onClick = { editEndType = RecurrenceEndType.UNTIL_DATE },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                )
                Text("On date:", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                if (editEndType == RecurrenceEndType.UNTIL_DATE) {
                    OutlinedTextField(
                        value = editEndDateText,
                        onValueChange = { editEndDateText = it },
                        placeholder = { Text("yyyy-MM-dd", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.width(140.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            cursorColor = colors.accent,
                            focusedTextColor = colors.textOnCardPrimary,
                            unfocusedTextColor = colors.textOnCardPrimary
                        )
                    )
                }
            }
            Row(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .clickable { editEndType = RecurrenceEndType.AFTER_OCCURRENCES }.padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RadioButton(
                    selected = editEndType == RecurrenceEndType.AFTER_OCCURRENCES,
                    onClick = { editEndType = RecurrenceEndType.AFTER_OCCURRENCES },
                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent)
                )
                Text("After", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                if (editEndType == RecurrenceEndType.AFTER_OCCURRENCES) {
                    OutlinedTextField(
                        value = editMaxOccurrencesText,
                        onValueChange = { editMaxOccurrencesText = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("N", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.borderOnCard,
                            cursorColor = colors.accent,
                            focusedTextColor = colors.textOnCardPrimary,
                            unfocusedTextColor = colors.textOnCardPrimary
                        )
                    )
                    Text("occurrences", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val updatedRule = RecurrenceRule(
                        frequency = editFrequency,
                        daysOfWeek = editDays,
                        endType = editEndType,
                        endDate = if (editEndType == RecurrenceEndType.UNTIL_DATE) parseDateToEpoch(editEndDateText) else null,
                        maxOccurrences = if (editEndType == RecurrenceEndType.AFTER_OCCURRENCES) editMaxOccurrencesText.toIntOrNull() else null,
                    )
                    taskViewModel.updateTask(template.copy(title = editTitle, recurrenceRule = updatedRule))
                    taskViewModel.updateRecurrenceRule(template.id, updatedRule)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
            Button(
                onClick = { taskViewModel.deleteTemplate(template.id); onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.destructive),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete Template")
            }
        }
    }
}

@Composable
private fun TaskDetailPanel(
    task: Task,
    taskViewModel: TaskViewModel,
    timerViewModel: TimerViewModel,
    tagViewModel: TagViewModel,
    onClose: () -> Unit,
    colors: com.timewgui.ui.theme.TimewColors
) {
    var editTitle by remember(task.id) { mutableStateOf(task.title) }
    var editNote by remember(task.id) { mutableStateOf(task.note) }
    var editStatus by remember(task.id) { mutableStateOf(task.status) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
            .background(colors.cardSurface)
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close detail panel",
                    tint = colors.textOnCardSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        OutlinedTextField(
            value = editTitle,
            onValueChange = { newTitle ->
                editTitle = newTitle
                taskViewModel.updateTask(task.copy(title = newTitle))
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

        Text(
            text = "Tag: ${task.tag}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textOnCardSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        TagSelector(
            selectedTags = task.contextTags,
            availableTags = tagViewModel.availableTags,
            onTagAdded = { tag ->
                taskViewModel.updateTask(task.copy(contextTags = task.contextTags + tag))
            },
            onTagRemoved = { tag ->
                taskViewModel.updateTask(task.copy(contextTags = task.contextTags - tag))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Status", style = MaterialTheme.typography.bodySmall, color = colors.textOnCardSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TaskStatus.entries.forEach { status ->
                val isActive = editStatus == status
                Button(
                    onClick = {
                        editStatus = status
                        taskViewModel.updateStatus(task.id, status)
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

        OutlinedTextField(
            value = editNote,
            onValueChange = { newNote ->
                editNote = newNote
                taskViewModel.updateTask(task.copy(note = newNote))
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { taskViewModel.startTimerForTask(task, timerViewModel) },
                enabled = task.status != TaskStatus.DONE && task.status != TaskStatus.ARCHIVED,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Start Timer")
            }
            Button(
                onClick = { taskViewModel.deleteTask(task.id); onClose() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.destructive),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete")
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
                taskViewModel.updateStatus(task.id, if (checked) TaskStatus.DONE else TaskStatus.TODO)
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

        // Recurring instance indicator
        if (task.recurrenceTemplateId != null) {
            Text(
                text = "\u21BB",
                color = colors.accent,
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

private fun ruleDescription(rule: RecurrenceRule): String {
    val freq = when (rule.frequency) {
        RecurrenceFrequency.DAILY -> "Daily"
        RecurrenceFrequency.WEEKDAYS -> "Weekdays"
        RecurrenceFrequency.WEEKLY -> {
            val days = rule.daysOfWeek.sorted().joinToString("/") { dayShortName(it) }
            "Weekly $days"
        }
    }
    val end = when (rule.endType) {
        RecurrenceEndType.FOREVER -> "Forever"
        RecurrenceEndType.UNTIL_DATE -> {
            val date = rule.endDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMM yyyy"))
            } ?: "?"
            "Until $date"
        }
        RecurrenceEndType.AFTER_OCCURRENCES -> "${rule.maxOccurrences ?: "?"} occurrences"
    }
    return "$freq · $end"
}

private fun dayShortName(day: Int): String = when (day) {
    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"; 5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> "?"
}

private fun parseDateToEpoch(dateStr: String): Long? = try {
    LocalDate.parse(dateStr).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (_: Exception) {
    null
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
