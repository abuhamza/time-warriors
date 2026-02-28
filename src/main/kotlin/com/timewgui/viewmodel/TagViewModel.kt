package com.timewgui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.timewgui.domain.cli.TimewCli
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

private val TAG_COLOR_PALETTE = listOf(
    Color(0xFFE57373), // Red
    Color(0xFF81C784), // Green
    Color(0xFF64B5F6), // Blue
    Color(0xFFFFB74D), // Orange
    Color(0xFFBA68C8), // Purple
    Color(0xFF4DD0E1), // Cyan
    Color(0xFFFF8A65), // Deep Orange
    Color(0xFF9575CD), // Deep Purple
)

class TagViewModel(
    private val timewCli: TimewCli,
    private val onError: (String) -> Unit = {}
) {
    var availableTags: List<String> by mutableStateOf(emptyList())
        private set

    var tagColors: Map<String, Color> by mutableStateOf(loadPersistedColors())
        private set

    var archivedTags: Set<String> by mutableStateOf(loadArchivedTags())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch { refreshTags() }
    }

    fun refreshTags() {
        scope.launch {
            timewCli.exportIntervals(range = null, tags = emptyList())
                .onSuccess { intervalList ->
                    val tags = intervalList
                        .flatMap { it.tags }
                        .distinct()
                        .sorted()
                    availableTags = tags
                    ensureColorsForTags(tags)
                }
                .onFailure { e -> onError(e.message ?: "Failed to load tags") }
        }
    }

    fun getColorForTag(tag: String): Color {
        return tagColors[tag] ?: assignColorFromPalette(tag)
    }

    fun assignColor(tag: String, color: Color) {
        tagColors = tagColors + (tag to color)
        persistColors(tagColors)
    }

    fun filterSuggestions(query: String): List<String> {
        val active = availableTags.filter { it !in archivedTags }
        if (query.isBlank()) return active
        val lower = query.lowercase()
        return active.filter { it.lowercase().contains(lower) }
    }

    fun archiveTag(tag: String) {
        archivedTags = archivedTags + tag
        persistArchivedTags()
    }

    fun unarchiveTag(tag: String) {
        archivedTags = archivedTags - tag
        persistArchivedTags()
    }

    fun isTagArchived(tag: String): Boolean = tag in archivedTags

    private fun assignColorFromPalette(tag: String): Color {
        val index = (tagColors.size + availableTags.indexOf(tag).coerceAtLeast(0)) % TAG_COLOR_PALETTE.size
        val color = TAG_COLOR_PALETTE[index]
        assignColor(tag, color)
        return color
    }

    private fun ensureColorsForTags(tags: List<String>) {
        val missing = tags.filter { it !in tagColors }
        if (missing.isNotEmpty()) {
            val existingCount = tagColors.size
            val newColors = missing.mapIndexed { i, tag ->
                tag to TAG_COLOR_PALETTE[(existingCount + i) % TAG_COLOR_PALETTE.size]
            }
            tagColors = tagColors + newColors
            persistColors(tagColors)
        }
    }

    private fun persistColors(colors: Map<String, Color>) {
        val prefs = Preferences.userNodeForPackage(TagViewModel::class.java)
        colors.forEach { (tag, color) ->
            prefs.putLong("tag_color_$tag", color.value.toLong())
        }
        prefs.flush()
    }

    private fun loadPersistedColors(): Map<String, Color> {
        val prefs = Preferences.userNodeForPackage(TagViewModel::class.java)
        return try {
            prefs.keys()
                ?.asSequence()
                ?.filter { it.startsWith("tag_color_") }
                ?.mapNotNull { key ->
                    val tag = key.removePrefix("tag_color_")
                    val value = prefs.getLong(key, -1L)
                    if (value >= 0) tag to Color(value.toULong()) else null
                }
                ?.toMap()
                ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun persistArchivedTags() {
        val prefs = Preferences.userNodeForPackage(TagViewModel::class.java)
        prefs.put("archived_tags", archivedTags.joinToString(","))
        prefs.flush()
    }

    private fun loadArchivedTags(): Set<String> {
        val prefs = Preferences.userNodeForPackage(TagViewModel::class.java)
        val raw = prefs.get("archived_tags", "")
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }
}
