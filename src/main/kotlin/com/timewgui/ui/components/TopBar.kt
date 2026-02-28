package com.timewgui.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewTypography

@Composable
fun TopBar(
    isTimerRunning: Boolean,
    elapsedTime: String,
    activeTags: List<String>,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.bgPrimary)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = colors.border,
                    start = Offset(0f, size.height - strokeWidth / 2),
                    end = Offset(size.width, size.height - strokeWidth / 2),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isTimerRunning) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PulsingDot(color = colors.success)
                Text(
                    text = elapsedTime,
                    style = TimewTypography.monospace,
                    color = colors.textPrimary
                )
                if (activeTags.isNotEmpty()) {
                    Text(
                        text = activeTags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        if (isTimerRunning) {
            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.destructive),
                shape = RoundedCornerShape(20.dp),
                elevation = null
            ) {
                Icon(
                    imageVector = Icons.Outlined.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop Timer", color = Color.White)
            }
        } else {
            Button(
                onClick = onStartClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                shape = RoundedCornerShape(20.dp),
                elevation = null
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Start",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Timer", color = Color.White)
            }
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .drawBehind {
                drawCircle(color = color.copy(alpha = alpha))
            }
    )
}
