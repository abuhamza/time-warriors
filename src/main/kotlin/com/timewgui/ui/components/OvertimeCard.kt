package com.timewgui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions
import com.timewgui.ui.theme.TimewTypography
import com.timewgui.viewmodel.OvertimeViewModel
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

@Composable
fun OvertimeCard(
    overtimeViewModel: OvertimeViewModel,
    startDate: LocalDate,
    onReviewUnreviewedDays: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val balance = overtimeViewModel.netBalance
    val todayBalance = overtimeViewModel.todayBalance
    val balanceColor = if (balance >= Duration.ZERO) colors.success else colors.destructive
    val todayColor = if (todayBalance >= Duration.ZERO) colors.success else colors.destructive
    val unreviewedCount = overtimeViewModel.unreviewedDays.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TimewDimensions.borderRadiusCard))
            .background(colors.cardSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overtime Balance",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textOnCardPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatSignedDuration(balance),
                style = TimewTypography.monospace,
                color = balanceColor
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Since $startDate",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textOnCardSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Today: ${formatSignedDuration(todayBalance)}",
                style = TimewTypography.monospace.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                color = todayColor
            )
        }

        if (unreviewedCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.warning.copy(alpha = 0.12f))
                    .clickable { onReviewUnreviewedDays() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = "Unreviewed days",
                    tint = colors.warning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$unreviewedCount unreviewed day${if (unreviewedCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.warning
                )
            }
        }
    }
}

private fun formatSignedDuration(duration: Duration): String {
    val sign = if (duration < Duration.ZERO) "-" else "+"
    val abs = duration.absoluteValue
    val totalMinutes = abs.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours == 0L) "${sign}${minutes}m" else "${sign}${hours}h ${minutes}m"
}
