package com.timewgui.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timewgui.domain.model.ExcludedDateRange
import com.timewgui.domain.model.Interval
import com.timewgui.ui.components.IntervalList
import com.timewgui.ui.components.OvertimeCard
import com.timewgui.ui.components.ProgressIndicator
import com.timewgui.ui.components.UnreviewedDaysDialog
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.viewmodel.AppState
import com.timewgui.viewmodel.OvertimeViewModel
import com.timewgui.viewmodel.TagViewModel
import com.timewgui.viewmodel.TimelineViewModel
import com.timewgui.viewmodel.TimerViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Duration.Companion.hours

private enum class PeriodTab { TODAY, THIS_WEEK, MONTH }

@Composable
fun DashboardScreen(
    timerViewModel: TimerViewModel,
    timelineViewModel: TimelineViewModel,
    tagViewModel: TagViewModel,
    overtimeViewModel: OvertimeViewModel,
    appState: AppState,
    onStartTimer: () -> Unit,
    onContinueInterval: (Interval) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    var selectedTab by remember { mutableStateOf(PeriodTab.TODAY) }
    var showUnreviewedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        timelineViewModel.jumpToToday()
        timelineViewModel.switchViewMode(com.timewgui.viewmodel.ViewMode.WEEK)
        overtimeViewModel.refresh()
    }

    val todayIntervals = remember(timelineViewModel.intervals) {
        timelineViewModel.getIntervalsForDate(today)
    }

    // Re-derive every tick so the progress bar updates while the timer is running
    val todayTotal = remember(todayIntervals, timerViewModel.elapsedTime) {
        todayIntervals.fold(kotlin.time.Duration.ZERO) { acc, i -> acc + i.duration }
    }

    val weeklyTotal = remember(timelineViewModel.intervals, timerViewModel.elapsedTime) {
        timelineViewModel.getWeeklyTotal()
    }

    val displayIntervals = remember(timelineViewModel.intervals, selectedTab) {
        when (selectedTab) {
            PeriodTab.TODAY -> timelineViewModel.getIntervalsForDate(today)
            PeriodTab.THIS_WEEK -> timelineViewModel.intervals.take(20)
            PeriodTab.MONTH -> timelineViewModel.intervals.take(30)
        }
    }

    val topTagsThisWeek = remember(timelineViewModel.intervals) {
        timelineViewModel.intervals
            .flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }

    val weekNumber = remember(today) {
        val dayOfYear = today.dayOfYear
        ((dayOfYear - 1) / 7) + 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgPrimary)
    ) {
        SavannaGradientBanner(
            modifier = Modifier
                .fillMaxWidth()
                .height(TimewDimensions.bannerHeight)
        )

        ProgressIndicator(
            label = "Today",
            current = todayTotal,
            target = appState.dailyTargetHours.hours,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (appState.overtimeEnabled) {
            OvertimeCard(
                overtimeViewModel = overtimeViewModel,
                startDate = appState.overtimeStartDate,
                onReviewUnreviewedDays = { showUnreviewedDialog = true },
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp)
            )
        }

        // Period tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeriodTabItem(
                label = "Today",
                isSelected = selectedTab == PeriodTab.TODAY,
                onClick = { selectedTab = PeriodTab.TODAY }
            )
            Text(
                text = "›",
                color = colors.textTertiary,
                fontSize = 14.sp
            )
            PeriodTabItem(
                label = "This Week",
                isSelected = selectedTab == PeriodTab.THIS_WEEK,
                onClick = { selectedTab = PeriodTab.THIS_WEEK }
            )
            Text(
                text = "›",
                color = colors.textTertiary,
                fontSize = 14.sp
            )
            PeriodTabItem(
                label = "$weekNumber Week",
                isSelected = selectedTab == PeriodTab.MONTH,
                onClick = { selectedTab = PeriodTab.MONTH }
            )
        }

        // Interval list card (takes remaining vertical space)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
                .background(colors.cardSurface)
                .padding(vertical = 8.dp)
        ) {
            IntervalList(
                intervals = displayIntervals,
                getTagColor = tagViewModel::getColorForTag,
                onIntervalClicked = { interval ->
                    if (!interval.isActive) onContinueInterval(interval)
                }
            )
        }

        // Tag chips
        if (topTagsThisWeek.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topTagsThisWeek.forEach { tag ->
                    val tagColor = tagViewModel.getColorForTag(tag)
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(tagColor)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showUnreviewedDialog && overtimeViewModel.unreviewedDays.isNotEmpty()) {
        UnreviewedDaysDialog(
            unreviewedDays = overtimeViewModel.unreviewedDays,
            onExcludeSelected = { dates, label ->
                dates.forEach { date ->
                    appState.addExcludedDateRange(
                        ExcludedDateRange(start = date, end = date, label = label)
                    )
                }
                overtimeViewModel.refresh()
                showUnreviewedDialog = false
            },
            onDismiss = { showUnreviewedDialog = false }
        )
    }
}

@Composable
private fun PeriodTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTimewColors.current
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) colors.textPrimary else colors.textSecondary,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Draws a warm gradient banner evoking an African savanna sunset,
 * with a sun circle and simplified tree silhouettes.
 */
@Composable
private fun SavannaGradientBanner(modifier: Modifier = Modifier) {
    val skyTop = Color(0xFFD4760A)
    val skyMid = Color(0xFFE8952A)
    val skyHorizon = Color(0xFFF5C84A)
    val sunColor = Color(0xFFF5D870)
    val groundColor = Color(0xFF2D1B0E)
    val treeSilhouette = Color(0xFF1A0F08)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val horizonY = h * 0.65f

        // Sky gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid, skyHorizon),
                startY = 0f,
                endY = horizonY
            ),
            size = size.copy(height = horizonY)
        )

        // Ground
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundColor.copy(alpha = 0.6f), groundColor),
                startY = horizonY,
                endY = h
            ),
            topLeft = Offset(0f, horizonY),
            size = size.copy(height = h - horizonY)
        )

        // Sun
        val sunRadius = h * 0.18f
        val sunCenterX = w * 0.55f
        val sunCenterY = horizonY - sunRadius * 0.3f
        drawCircle(
            color = sunColor.copy(alpha = 0.9f),
            radius = sunRadius,
            center = Offset(sunCenterX, sunCenterY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = sunRadius * 0.7f,
            center = Offset(sunCenterX, sunCenterY)
        )

        // Simplified acacia tree silhouettes
        fun drawTree(cx: Float, baseY: Float, trunkH: Float, canopyW: Float, canopyH: Float) {
            val trunkWidth = canopyW * 0.08f
            drawRect(
                color = treeSilhouette,
                topLeft = Offset(cx - trunkWidth / 2, baseY - trunkH),
                size = androidx.compose.ui.geometry.Size(trunkWidth, trunkH)
            )
            val canopyPath = Path().apply {
                moveTo(cx - canopyW / 2, baseY - trunkH)
                quadraticTo(cx - canopyW * 0.6f, baseY - trunkH - canopyH * 0.5f, cx, baseY - trunkH - canopyH)
                quadraticTo(cx + canopyW * 0.6f, baseY - trunkH - canopyH * 0.5f, cx + canopyW / 2, baseY - trunkH)
                close()
            }
            drawPath(canopyPath, color = treeSilhouette, style = Fill)
        }

        drawTree(w * 0.08f, h, h * 0.35f, w * 0.12f, h * 0.18f)
        drawTree(w * 0.22f, h, h * 0.25f, w * 0.08f, h * 0.12f)
        drawTree(w * 0.78f, h, h * 0.30f, w * 0.10f, h * 0.15f)
        drawTree(w * 0.92f, h, h * 0.38f, w * 0.14f, h * 0.20f)

        // Distant grass/horizon line
        val grassPath = Path().apply {
            moveTo(0f, horizonY)
            var x = 0f
            while (x < w) {
                val peakY = horizonY - (2f + (x * 7f % 5f))
                lineTo(x + 3f, peakY)
                lineTo(x + 6f, horizonY + 1f)
                x += 6f
            }
            lineTo(w, horizonY)
            close()
        }
        drawPath(grassPath, color = treeSilhouette.copy(alpha = 0.4f), style = Fill)
    }
}
