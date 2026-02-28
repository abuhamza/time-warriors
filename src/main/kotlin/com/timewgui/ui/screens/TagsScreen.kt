package com.timewgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.timewgui.ui.components.IntervalList
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.viewmodel.TagViewModel
import com.timewgui.viewmodel.TimelineViewModel
import com.timewgui.viewmodel.ViewMode

@Composable
fun TagsScreen(
    tagViewModel: TagViewModel,
    timelineViewModel: TimelineViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val selectedTagState = remember { mutableStateOf<String?>(null) }
    val selectedTag = selectedTagState.value
    var showArchived by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        timelineViewModel.jumpToToday()
        timelineViewModel.switchViewMode(ViewMode.WEEK)
    }

    val filteredIntervals = remember(selectedTagState.value, timelineViewModel.intervals) {
        selectedTagState.value?.let { tag ->
            timelineViewModel.intervals.filter { tag in it.tags }
        } ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgPrimary)
            .padding(TimewDimensions.sectionGap),
        verticalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
    ) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TimewDimensions.sectionGap)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "All Tags",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !showArchived,
                        onClick = { showArchived = false },
                        label = { Text("Active") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.15f),
                            selectedLabelColor = colors.accent
                        )
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = true },
                        label = { Text("Archived") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.accent.copy(alpha = 0.15f),
                            selectedLabelColor = colors.accent
                        )
                    )
                }
                val displayedTags = if (showArchived) {
                    tagViewModel.availableTags.filter { tagViewModel.isTagArchived(it) }
                } else {
                    tagViewModel.availableTags.filter { !tagViewModel.isTagArchived(it) }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedTags) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedTag == tag) colors.accent.copy(alpha = 0.15f)
                                    else colors.cardSurface
                                )
                                .clickable { selectedTagState.value = if (selectedTag == tag) null else tag }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(tagViewModel.getColorForTag(tag))
                            )
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textOnCardPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    if (tagViewModel.isTagArchived(tag)) tagViewModel.unarchiveTag(tag)
                                    else tagViewModel.archiveTag(tag)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (tagViewModel.isTagArchived(tag)) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                    contentDescription = if (tagViewModel.isTagArchived(tag)) "Unarchive" else "Archive",
                                    tint = colors.textOnCardSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedTag?.let { "Intervals with \"$it\"" } ?: "Select a tag",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (selectedTag != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                            .background(colors.cardSurface)
                    ) {
                        IntervalList(
                            intervals = filteredIntervals,
                            getTagColor = tagViewModel::getColorForTag,
                            onIntervalClicked = { }
                        )
                    }
                } else {
                    Text(
                        text = "Click a tag to see its intervals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
