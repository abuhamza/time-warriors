package com.timewgui.domain.cli

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TimewCliTest {

    @Test
    fun `non-existent command returns Result failure with TimewException`() = runTest {
        val cli = TimewCli(timewCommand = "timew-nonexistent-command-xyz")
        val result = cli.exportIntervals()
        assertTrue(result.isFailure, "Should fail for non-existent command")
        val exception = result.exceptionOrNull()
        assertTrue(exception is TimewException, "Should be TimewException, was ${exception?.javaClass}")
    }
}
