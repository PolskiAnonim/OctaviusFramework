package org.octavius.ui.timeline

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

data class TimelineTheme(
    val axis: AxisStyle,
    val grid: GridStyle,
    val hover: HoverStyle,
    val laneLabel: LaneLabelStyle,
    val selection: SelectionStyle,
    val laneSeparatorColor: Color,
    val backgroundColor: Color,
) {
    data class AxisStyle(
        val labelColor: Color,
        val tickColor: Color,
        val textStyle: TextStyle,
        val tickHeight: Float = 8f,
    )

    data class GridStyle(
        val lineColor: Color,
    )

    data class HoverStyle(
        val lineColor: Color,
        val labelBgColor: Color,
        val labelTextStyle: TextStyle,
        val tooltipTitleStyle: TextStyle = labelTextStyle,
        val tooltipDescriptionStyle: TextStyle = labelTextStyle,
    )

    data class LaneLabelStyle(
        val bgColor: Color,
        val textColor: Color,
        val textStyle: TextStyle,
    )

    data class SelectionStyle(
        val overlayColor: Color,
        val borderColor: Color,
        val badgeBgColor: Color,
        val badgeTextStyle: TextStyle,
        val badgeIconColor: Color,
    )
}

@Composable
fun rememberTimelineTheme(): TimelineTheme {
    val colors = MaterialTheme.colorScheme
    return TimelineTheme(
        axis = TimelineTheme.AxisStyle(
            labelColor = colors.onSurfaceVariant,
            tickColor = colors.outline,
            textStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
        ),
        grid = TimelineTheme.GridStyle(
            lineColor = colors.outlineVariant,
        ),
        hover = TimelineTheme.HoverStyle(
            lineColor = colors.onSurface.copy(alpha = 0.5f),
            labelBgColor = colors.inverseSurface,
            labelTextStyle = TextStyle(color = colors.inverseOnSurface, fontSize = 12.sp),
            tooltipTitleStyle = TextStyle(color = colors.inverseOnSurface, fontSize = 13.sp),
            tooltipDescriptionStyle = TextStyle(color = colors.inverseOnSurface.copy(alpha = 0.7f), fontSize = 11.sp),
        ),
        laneLabel = TimelineTheme.LaneLabelStyle(
            bgColor = Color.Black.copy(alpha = 0.4f),
            textColor = Color.White.copy(alpha = 0.7f),
            textStyle = TextStyle(color = colors.onSurface, fontSize = 12.sp),
        ),
        selection = TimelineTheme.SelectionStyle(
            overlayColor = Color.Black.copy(alpha = 0.28f),
            borderColor = colors.primary.copy(alpha = 0.9f),
            badgeBgColor = colors.primaryContainer,
            badgeTextStyle = TextStyle(color = colors.onPrimaryContainer, fontSize = 12.sp),
            badgeIconColor = colors.onPrimaryContainer,
        ),
        laneSeparatorColor = colors.outline,
        backgroundColor = colors.background,
    )
}
