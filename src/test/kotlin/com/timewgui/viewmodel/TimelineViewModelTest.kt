package com.timewgui.viewmodel

import com.timewgui.domain.model.Interval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.datetime.minus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var stubCli: StubTimewCli

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stubCli = StubTimewCli()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `navigateNext in DAY mode advances by 1 day`() = runTest(testDispatcher) {
        val vm = TimelineViewModel(stubCli)
        advanceUntilIdle()
        val before = vm.selectedDate
        vm.navigateNext()
        advanceUntilIdle()
        assertEquals(before.plus(DatePeriod(days = 1)), vm.selectedDate)
    }

    @Test
    fun `navigateNext in WEEK mode advances by 7 days`() = runTest(testDispatcher) {
        val vm = TimelineViewModel(stubCli)
        advanceUntilIdle()
        vm.switchViewMode(ViewMode.WEEK)
        advanceUntilIdle()
        val before = vm.selectedDate
        vm.navigateNext()
        advanceUntilIdle()
        assertEquals(before.plus(DatePeriod(days = 7)), vm.selectedDate)
    }

    @Test
    fun `navigatePrevious in DAY mode goes back 1 day`() = runTest(testDispatcher) {
        val vm = TimelineViewModel(stubCli)
        advanceUntilIdle()
        val before = vm.selectedDate
        vm.navigatePrevious()
        advanceUntilIdle()
        assertEquals(before.minus(DatePeriod(days = 1)), vm.selectedDate)
    }

    @Test
    fun `jumpToDate sets exact date`() = runTest(testDispatcher) {
        val vm = TimelineViewModel(stubCli)
        advanceUntilIdle()
        val target = LocalDate(2026, 6, 15)
        vm.jumpToDate(target)
        advanceUntilIdle()
        assertEquals(target, vm.selectedDate)
    }

    @Test
    fun `switchViewMode changes mode`() = runTest(testDispatcher) {
        val vm = TimelineViewModel(stubCli)
        advanceUntilIdle()
        assertEquals(ViewMode.DAY, vm.viewMode)
        vm.switchViewMode(ViewMode.WEEK)
        advanceUntilIdle()
        assertEquals(ViewMode.WEEK, vm.viewMode)
    }

    @Test
    fun `getIntervalsForDate filters correctly`() = runTest(testDispatcher) {
        val matchingInterval = Interval(
            id = 1,
            start = Instant.parse("2026-02-27T08:00:00Z"),
            end = Instant.parse("2026-02-27T09:00:00Z"),
            tags = listOf("work")
        )
        val nonMatchingInterval = Interval(
            id = 2,
            start = Instant.parse("2026-02-26T08:00:00Z"),
            end = Instant.parse("2026-02-26T09:00:00Z"),
            tags = listOf("other")
        )
        stubCli.exportResult = Result.success(listOf(matchingInterval, nonMatchingInterval))

        val vm = TimelineViewModel(stubCli)
        advanceUntilIdle()

        val date = LocalDate(2026, 2, 27)
        vm.jumpToDate(date)
        advanceUntilIdle()

        val filtered = vm.getIntervalsForDate(date)
        assertEquals(1, filtered.size)
        assertEquals("work", filtered.first().tags.first())
    }
}
