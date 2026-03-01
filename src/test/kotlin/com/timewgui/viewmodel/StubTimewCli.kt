package com.timewgui.viewmodel

import com.timewgui.domain.cli.TimewCli
import com.timewgui.domain.model.Interval

/**
 * Test double for TimewCli that returns configurable results.
 * Used instead of MockK because MockK cannot mock functions returning kotlin.Result.
 */
class StubTimewCli(
    var exportResult: Result<List<Interval>> = Result.success(emptyList())
) : TimewCli(timewCommand = "stub") {

    override suspend fun exportIntervals(
        range: String?,
        tags: List<String>
    ): Result<List<Interval>> = exportResult
}
