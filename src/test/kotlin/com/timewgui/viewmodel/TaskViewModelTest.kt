package com.timewgui.viewmodel

import com.timewgui.domain.api.AiToolsClient
import com.timewgui.domain.model.GeneratedTask
import com.timewgui.domain.model.Task
import com.timewgui.domain.model.TaskStatus
import com.timewgui.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var stubCli: StubTimewCli
    private lateinit var tempDir: File
    private lateinit var taskRepository: TaskRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stubCli = StubTimewCli()
        tempDir = File(System.getProperty("java.io.tmpdir"), "timewgui-test-${System.nanoTime()}")
        tempDir.mkdirs()
        taskRepository = object : TaskRepository() {
            private var stored: List<Task> = emptyList()
            override fun load(): List<Task> = stored
            override fun save(tasks: List<Task>) { stored = tasks }
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    @Test
    fun `createTask adds task with TODO status and generated tag`() = runTest(testDispatcher) {
        val vm = TaskViewModel(taskRepository, stubCli)
        advanceUntilIdle()

        vm.createTask("Fix Login Bug", listOf("frontend"))
        advanceUntilIdle()

        assertEquals(1, vm.tasks.size)
        val task = vm.tasks.first()
        assertEquals("Fix Login Bug", task.title)
        assertEquals(TaskStatus.TODO, task.status)
        assertEquals("task:fix-login-bug", task.tag)
        assertEquals(listOf("frontend"), task.contextTags)
        assertNull(task.completedAt)
    }

    @Test
    fun `updateStatus to DONE sets completedAt`() = runTest(testDispatcher) {
        val vm = TaskViewModel(taskRepository, stubCli)
        advanceUntilIdle()

        vm.createTask("My Task", emptyList())
        advanceUntilIdle()
        val taskId = vm.tasks.first().id

        vm.updateStatus(taskId, TaskStatus.DONE)
        advanceUntilIdle()

        val updated = vm.tasks.first()
        assertEquals(TaskStatus.DONE, updated.status)
        assertNotNull(updated.completedAt, "completedAt should be set when DONE")
    }

    @Test
    fun `updateStatus to IN_PROGRESS clears completedAt`() = runTest(testDispatcher) {
        val vm = TaskViewModel(taskRepository, stubCli)
        advanceUntilIdle()

        vm.createTask("My Task", emptyList())
        advanceUntilIdle()
        val taskId = vm.tasks.first().id

        vm.updateStatus(taskId, TaskStatus.DONE)
        advanceUntilIdle()
        assertNotNull(vm.tasks.first().completedAt)

        vm.updateStatus(taskId, TaskStatus.IN_PROGRESS)
        advanceUntilIdle()

        val updated = vm.tasks.first()
        assertEquals(TaskStatus.IN_PROGRESS, updated.status)
        assertNull(updated.completedAt, "completedAt should be cleared for IN_PROGRESS")
    }

    @Test
    fun `deleteTask removes task from list`() = runTest(testDispatcher) {
        val vm = TaskViewModel(taskRepository, stubCli)
        advanceUntilIdle()

        vm.createTask("Task 1", emptyList())
        vm.createTask("Task 2", emptyList())
        advanceUntilIdle()
        assertEquals(2, vm.tasks.size)

        val taskToDelete = vm.tasks.first().id
        vm.deleteTask(taskToDelete)
        advanceUntilIdle()

        assertEquals(1, vm.tasks.size)
        assertTrue(vm.tasks.none { it.id == taskToDelete })
    }

    @Test
    fun `generateTasksFromBrainDump sets generatedTasks on success`() = runTest(testDispatcher) {
        val mockClient = object : AiToolsClient("http://test", "token") {
            override suspend fun generateTasks(text: String, existingTags: List<String>): Result<List<GeneratedTask>> {
                return Result.success(listOf(
                    GeneratedTask("Buy groceries", listOf("errands")),
                    GeneratedTask("Finish report", listOf("work")),
                ))
            }
        }
        val vm = TaskViewModel(taskRepository, stubCli, aiToolsClient = mockClient)
        advanceUntilIdle()

        vm.generateTasksFromBrainDump("I need to buy groceries and finish the report")
        advanceUntilIdle()

        assertEquals(2, vm.generatedTasks.size)
        assertEquals("Buy groceries", vm.generatedTasks[0].title)
        assertEquals(false, vm.isGenerating)
        assertNull(vm.generationError)
    }

    @Test
    fun `createTasksFromGenerated converts GeneratedTasks to Tasks`() = runTest(testDispatcher) {
        val vm = TaskViewModel(taskRepository, stubCli)
        advanceUntilIdle()

        val generated = listOf(
            GeneratedTask("Buy groceries", listOf("errands")),
            GeneratedTask("Call mom", listOf("family")),
        )
        vm.createTasksFromGenerated(generated)
        advanceUntilIdle()

        assertEquals(2, vm.tasks.size)
        assertEquals("Buy groceries", vm.tasks[0].title)
        assertEquals("task:buy-groceries", vm.tasks[0].tag)
        assertEquals(listOf("errands"), vm.tasks[0].contextTags)
        assertEquals(TaskStatus.TODO, vm.tasks[0].status)
        assertEquals("Call mom", vm.tasks[1].title)
        assertEquals(listOf("family"), vm.tasks[1].contextTags)
    }

    @Test
    fun `generateTasksFromBrainDump sets error when client not configured`() = runTest(testDispatcher) {
        val vm = TaskViewModel(taskRepository, stubCli, aiToolsClient = null)
        advanceUntilIdle()

        vm.generateTasksFromBrainDump("some text")
        advanceUntilIdle()

        assertNotNull(vm.generationError)
        assertTrue(vm.generationError!!.contains("not configured"))
        assertEquals(false, vm.isGenerating)
    }
}
