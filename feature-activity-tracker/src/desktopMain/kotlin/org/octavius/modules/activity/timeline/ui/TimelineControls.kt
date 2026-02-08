package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.octavius.localization.Tr

@Composable
fun TimelineControls(
    selectedDate: LocalDate,
    zoomLevel: Float,
    onDateChange: (LocalDate) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date navigation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    val newDate = selectedDate.minus(DatePeriod(days = 1))
                    onDateChange(newDate)
                }
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
            }

            Text(
                text = "${selectedDate.dayOfMonth}.${selectedDate.monthNumber}.${selectedDate.year}",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = {
                    val newDate = selectedDate.plus(DatePeriod(days = 1))
                    onDateChange(newDate)
                }
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
            }
        }

        // Zoom slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = Tr.ActivityTracker.Timeline.zoom(),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = zoomLevel,
                onValueChange = onZoomChange,
                valueRange = 30f..200f,
                modifier = Modifier.width(150.dp)
            )
        }
    }
}
