package com.timewgui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timewgui.domain.model.Interval
import com.timewgui.domain.model.Task
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.viewmodel.SummaryViewModel
import com.timewgui.viewmodel.SummaryPeriod
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SummaryCard(
    viewModel: SummaryViewModel,
    intervals: List<Interval>,
    tasks: List<Task>,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val coroutineScope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
            .background(colors.cardSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Summary",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textOnCardPrimary,
                modifier = Modifier.weight(1f)
            )

            if (viewModel.summary.isNotBlank()) {
                IconButton(
                    onClick = {
                        val selection = java.awt.datatransfer.StringSelection(viewModel.summary)
                        java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            .setContents(selection, selection)
                        copied = true
                        coroutineScope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy summary",
                        tint = if (copied) colors.success else colors.textOnCardSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = { viewModel.clear() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear summary",
                        tint = colors.textOnCardSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Period buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryPeriod.entries.forEach { period ->
                val isSelected = viewModel.selectedPeriod == period
                val isLoading = viewModel.isLoading && isSelected
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colors.accent else colors.textOnCardSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) colors.accent.copy(alpha = 0.12f)
                            else colors.bgSecondary
                        )
                        .clickable(enabled = !viewModel.isLoading) {
                            viewModel.generateSummary(period, intervals, tasks)
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Loading indicator
        AnimatedVisibility(
            visible = viewModel.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = colors.accent
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Generating summary\u2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textOnCardSecondary
                )
            }
        }

        // Summary text
        AnimatedVisibility(
            visible = viewModel.summary.isNotBlank() && !viewModel.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = viewModel.summary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}
