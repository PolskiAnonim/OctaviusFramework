package org.octavius.modules.activity.weekly.ui

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.octavius.modules.activity.weekly.DayData

@Composable
fun WeeklyStackedBar(
    days: List<DayData>,
    maxSeconds: Long,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        // Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 30f // Leave space for labels
            val barWidth = (canvasWidth - 40f) / 7f
            val gap = 8f
            val effectiveBarWidth = barWidth - gap

            val effectiveMax = if (maxSeconds > 0) maxSeconds else 1L

            days.forEachIndexed { index, day ->
                val x = 20f + index * barWidth + gap / 2
                var y = canvasHeight

                day.categories.forEach { category ->
                    val height = (category.seconds.toFloat() / effectiveMax) * canvasHeight
                    val color = parseColor(category.color)

                    drawRect(
                        color = color,
                        topLeft = Offset(x, y - height),
                        size = Size(effectiveBarWidth, height)
                    )

                    y -= height
                }

                // Day label
                val textLayout = textMeasurer.measure(
                    text = day.dayOfWeek,
                    style = TextStyle(fontSize = 11.sp, color = Color.Gray)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(
                        x + (effectiveBarWidth - textLayout.size.width) / 2,
                        canvasHeight + 8f
                    )
                )
            }

            // Y-axis labels
            val gridLines = 4
            for (i in 0..gridLines) {
                val yPos = canvasHeight - (i.toFloat() / gridLines) * canvasHeight
                val hours = (effectiveMax * i / gridLines / 3600).toInt()
                val label = "${hours}h"
                val textLayout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(0f, yPos - textLayout.size.height / 2)
                )

                // Grid line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(18f, yPos),
                    end = Offset(canvasWidth, yPos),
                    strokeWidth = 1f
                )
            }
        }
    }
}

@Composable
fun WeeklyLegend(
    categories: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        categories.forEach { (name, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(color = parseColor(color))
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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
