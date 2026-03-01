package com.timewgui.viewmodel

import com.timewgui.domain.model.Interval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TagViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var stubCli: StubTimewCli

    private val sampleIntervals = listOf(
        Interval(1, Instant.parse("2026-02-27T08:00:00Z"), Instant.parse("2026-02-27T09:00:00Z"), listOf("coding", "review")),
        Interval(2, Instant.parse("2026-02-27T09:00:00Z"), Instant.parse("2026-02-27T10:00:00Z"), listOf("meeting")),
        Interval(3, Instant.parse("2026-02-27T10:00:00Z"), Instant.parse("2026-02-27T11:00:00Z"), listOf("coding")),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stubCli = StubTimewCli(exportResult = Result.success(sampleIntervals))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filterSuggestions with empty query returns all non-archived tags`() = runTest(testDispatcher) {
        val vm = TagViewModel(stubCli)
        advanceUntilIdle()

        val suggestions = vm.filterSuggestions("")
        assertEquals(listOf("coding", "meeting", "review"), suggestions)
    }

    @Test
    fun `filterSuggestions with query filters by substring`() = runTest(testDispatcher) {
        val vm = TagViewModel(stubCli)
        advanceUntilIdle()

        val suggestions = vm.filterSuggestions("cod")
        assertEquals(listOf("coding"), suggestions)
    }

    @Test
    fun `archiveTag and unarchiveTag toggle`() = runTest(testDispatcher) {
        val vm = TagViewModel(stubCli)
        advanceUntilIdle()

        vm.archiveTag("meeting")
        assertTrue(vm.isTagArchived("meeting"))
        val afterArchive = vm.filterSuggestions("")
        assertFalse(afterArchive.contains("meeting"))

        vm.unarchiveTag("meeting")
        assertFalse(vm.isTagArchived("meeting"))
        val afterUnarchive = vm.filterSuggestions("")
        assertTrue(afterUnarchive.contains("meeting"))
    }
}
