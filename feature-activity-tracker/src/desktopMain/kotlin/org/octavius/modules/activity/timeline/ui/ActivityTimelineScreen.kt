package org.octavius.modules.activity.timeline.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import org.octavius.localization.Tr
import org.octavius.modules.activity.timeline.TimelineHandler
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineComponent
import org.octavius.ui.timeline.TimelineLane
import org.octavius.ui.timeline.rememberTimelineState
import kotlin.time.Clock

class ActivityTimelineScreen : Screen {

    override val title = Tr.ActivityTracker.Timeline.title()

    @Composable
    override fun Content() {
        val handler = remember { TimelineHandler() }
        val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val timelineState = rememberTimelineState()
        val scope = rememberCoroutineScope()
        val focusRequester = remember { FocusRequester() }

        var selectedDate by remember { mutableStateOf(today) }
        var lanes by remember { mutableStateOf<List<TimelineLane>>(emptyList()) }
        var isLoading by remember { mutableStateOf(false) }
        var isAutoFilling by remember { mutableStateOf(false) }
        var refreshKey by remember { mutableStateOf(0) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }

        LaunchedEffect(selectedDate, refreshKey) {
            isLoading = true
            lanes = withContext(Dispatchers.IO) { handler.loadLanes(selectedDate) }
            isLoading = false
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        fun deleteSelectedCategorySlot() {
            val id = timelineState.selectedBlock?.id ?: return
            showDeleteConfirmation = false
            scope.launch {
                withContext(Dispatchers.IO) { handler.deleteCategorySlot(id) }
                timelineState.clearBlockSelection()
                refreshKey++
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Delete) {
                        val block = timelineState.selectedBlock
                        if (block != null && block.id != null) {
                            showDeleteConfirmation = true
                            true
                        } else false
                    } else false
                },
        ) {
            DateNavigationBar(
                selectedDate = selectedDate,
                onPrevious = { selectedDate = selectedDate.minus(DatePeriod(days = 1)) },
                onNext = { selectedDate = selectedDate.plus(DatePeriod(days = 1)) },
                isAutoFilling = isAutoFilling,
                onAutoFillAll = {
                    scope.launch {
                        isAutoFilling = true
                        withContext(Dispatchers.IO) { handler.autoFillAllCategories(selectedDate) }
                        isAutoFilling = false
                        refreshKey++
                    }
                },
            )

            if (isLoading || isAutoFilling) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (!isLoading && !isAutoFilling && lanes.all { it.blocks.isEmpty() }) {
                EmptyState()
            } else {
                TimelineView(
                    lanes = lanes,
                    timelineState = timelineState,
                    showCurrentTime = selectedDate == today,
                    onDeleteRequest = { showDeleteConfirmation = true },
                    onAutoFillProcess = { processName ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                handler.autoFillCategoryForProcess(processName, selectedDate)
                            }
                            refreshKey++
                        }
                    }
                )
            }
        }

        if (showDeleteConfirmation) {
            DeleteSlotConfirmationDialog(
                block = timelineState.selectedBlock,
                onConfirm = ::deleteSelectedCategorySlot,
                onDismiss = { showDeleteConfirmation = false }
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = Tr.ActivityTracker.Timeline.noData(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TimelineView(
    lanes: List<TimelineLane>,
    timelineState: org.octavius.ui.timeline.TimelineState,
    showCurrentTime: Boolean,
    onDeleteRequest: () -> Unit,
    onAutoFillProcess: (String) -> Unit,
) {
    TimelineComponent(
        state = timelineState,
        showCurrentTime = showCurrentTime,
        lanes = lanes,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        blockContextMenuContent = { dismiss ->
            val block = timelineState.selectedBlock
            if (block != null) {
                if (block.id != null) {
                    DropdownMenuItem(
                        text = { Text("Usuń slot kategorii") },
                        onClick = {
                            dismiss()
                            onDeleteRequest()
                        },
                    )
                } else if (block.description.isNotBlank()) {
                    val processName = block.description
                    DropdownMenuItem(
                        text = { Text("Auto-fill kategorii dla: $processName") },
                        onClick = {
                            dismiss()
                            onAutoFillProcess(processName)
                        },
                    )
                }
            }
        },
    )
}
