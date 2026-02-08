package org.octavius.modules.activity.statistics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.octavius.modules.activity.statistics.PieSlice

@Composable
fun CategoryPieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie Chart
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2
            val strokeWidth = 40f
            val innerRadius = radius - strokeWidth

            var startAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = slice.percent * 360f
                val color = parseColor(slice.color)

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
                    style = Stroke(width = strokeWidth)
                )

                startAngle += sweepAngle
            }
        }

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                LegendItem(
                    label = slice.label,
                    color = parseColor(slice.color),
                    percent = slice.percent,
                    seconds = slice.value
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    percent: Float,
    seconds: Long
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${(percent * 100).toInt()}% (${formatDuration(seconds)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun parseColor(colorString: String): Color {
    return try {
        val color = colorString.removePrefix("#")
        val colorInt = when (color.length) {
            6 -> color.toLong(16).toInt() or 0xFF000000.toInt()
            8 -> color.toLong(16).toInt()
            else -> 0xFF6366F1.toInt()
        }
        Color(
            red = (colorInt shr 16 and 0xFF) / 255f,
            green = (colorInt shr 8 and 0xFF) / 255f,
            blue = (colorInt and 0xFF) / 255f,
            alpha = 1f
        )
    } catch (e: Exception) {
        Color(0xFF6366F1)
    }
}
