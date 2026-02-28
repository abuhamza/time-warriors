package com.timewgui.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timewgui.ui.navigation.Screen
import com.timewgui.ui.theme.LocalTimewColors
import com.timewgui.ui.theme.TimewDimensions

private val SidebarExpandedWidth = TimewDimensions.sidebarWidth
private val SidebarCollapsedWidth = TimewDimensions.sidebarWidthCollapsed
private val SidebarItemHeight = 44.dp
private val SidebarItemPadding = 14.dp
private val AnimationDuration = 150

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun Sidebar(
    currentScreen: Screen,
    expanded: Boolean,
    onScreenSelected: (Screen) -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTimewColors.current
    val mainItems = listOf(
        NavItem(Screen.DASHBOARD, "Home", Icons.Outlined.Home),
        NavItem(Screen.TIMELINE, "Timeline", Icons.Outlined.Timeline),
        NavItem(Screen.REPORTS, "Reports", Icons.Outlined.Assessment),
        NavItem(Screen.TAGS, "Tags", Icons.Outlined.Label),
        NavItem(Screen.TASKS, "Tasks", Icons.Outlined.CheckBox),
    )
    val settingsItem = NavItem(Screen.SETTINGS, "Settings", Icons.Outlined.Settings)

    Box(
        modifier = modifier
            .width(if (expanded) SidebarExpandedWidth else SidebarCollapsedWidth)
            .animateContentSize(animationSpec = tween(AnimationDuration, easing = EaseOut))
            .fillMaxHeight()
            .background(colors.bgSecondary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .clipToBounds()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // App branding
            Row(
                modifier = Modifier
                    .padding(horizontal = SidebarItemPadding)
                    .padding(bottom = 20.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "App icon",
                    tint = colors.accent,
                    modifier = Modifier.size(28.dp)
                )
                if (expanded) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TimeTrackAI",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            mainItems.forEach { item ->
                NavItemRow(
                    item = item,
                    isActive = currentScreen == item.screen,
                    expanded = expanded,
                    colors = colors,
                    onClick = { onScreenSelected(item.screen) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            NavItemRow(
                item = settingsItem,
                isActive = currentScreen == settingsItem.screen,
                expanded = expanded,
                colors = colors,
                onClick = { onScreenSelected(settingsItem.screen) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onToggleExpanded() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ChevronLeft else Icons.Outlined.ChevronRight,
                    contentDescription = if (expanded) "Collapse sidebar" else "Expand sidebar",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NavItemRow(
    item: NavItem,
    isActive: Boolean,
    expanded: Boolean,
    colors: com.timewgui.ui.theme.TimewColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsHoveredAsState().value
    val showHighlight = isActive || isHovered

    val bgColor = when {
        isActive -> colors.accent.copy(alpha = 0.15f)
        isHovered -> colors.bgTertiary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .hoverable(interactionSource = interactionSource)
            .background(bgColor)
            .height(SidebarItemHeight)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (isActive) colors.accent else colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        if (expanded) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.label,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isActive) colors.accent else colors.textSecondary
            )
        }
    }
}
