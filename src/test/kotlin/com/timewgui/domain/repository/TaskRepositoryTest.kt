package com.timewgui.domain.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class TaskRepositoryTest {

    private val repo = TaskRepository()

    @Test
    fun `generateTag normal title`() {
        assertEquals("task:fix-login-bug", repo.generateTag("Fix Login Bug"))
    }

    @Test
    fun `generateTag special characters replaced with hyphens`() {
        assertEquals("task:hello-world-2024", repo.generateTag("Hello, World! 2024"))
    }

    @Test
    fun `generateTag leading and trailing hyphens stripped`() {
        assertEquals("task:test-title", repo.generateTag("--test title--"))
    }

    @Test
    fun `generateTag long title truncated to 50 chars`() {
        val longTitle = "a".repeat(100)
        val tag = repo.generateTag(longTitle)
        val slug = tag.removePrefix("task:")
        assertTrue(slug.length <= 50, "Slug should be at most 50 chars, was ${slug.length}")
    }

    @Test
    fun `generateTag consecutive special chars collapsed to single hyphen`() {
        assertEquals("task:foo-bar", repo.generateTag("foo!!!bar"))
    }

    @Test
    fun `generateTag mixed case converted to lowercase`() {
        assertEquals("task:my-awesome-task", repo.generateTag("My AWESOME Task"))
    }

    private fun assertTrue(condition: Boolean, message: String) {
        kotlin.test.assertTrue(condition, message)
    }
}
