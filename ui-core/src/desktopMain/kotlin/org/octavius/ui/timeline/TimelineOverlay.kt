package org.octavius.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun TimelineSelectionOverlay(
    state: TimelineState,
    lanes: List<TimelineLane>,
    axisHeight: Float,
    componentWidth: Float,
    componentHeight: Float,
    badgeSize: IntSize,
    onBadgeSizeChanged: (IntSize) -> Unit,
    blockBadgeSize: IntSize,
    onBlockBadgeSizeChanged: (IntSize) -> Unit,
    selectionStyle: TimelineTheme.SelectionStyle,
    showContextMenu: Boolean,
    contextMenuAnchorPx: Offset,
    onShowContextMenu: (anchor: Offset) -> Unit,
    onDismissContextMenu: () -> Unit,
    showBlockContextMenu: Boolean,
    blockContextMenuAnchorPx: Offset,
    onShowBlockContextMenu: (anchor: Offset) -> Unit,
    onDismissBlockContextMenu: () -> Unit,
    contextMenuContent: @Composable ColumnScope.() -> Unit,
    blockContextMenuContent: @Composable ColumnScope.(onDismiss: () -> Unit) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // --- Badge selekcji obszaru ---
        val sel = state.selection
        if (sel != null) {
            val (badgeLeft, badgeTop) = badgeBounds(sel, state, componentWidth, badgeSize, axisHeight)
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(badgeLeft.roundToInt(), badgeTop.roundToInt()) }
                    .onSizeChanged(onBadgeSizeChanged)
            ) {
                SelectionBadge(
                    selection = sel,
                    style = selectionStyle,
                    onArrowClick = {
                        onShowContextMenu(Offset(badgeLeft, badgeTop + badgeSize.height))
                    },
                )
            }
        }

        // --- Badge zaznaczonego bloczku ---
        val block = state.selectedBlock
        val blockLaneIndex = state.selectedBlockLaneIndex
        if (block != null && blockLaneIndex != null) {
            val (blockBadgeLeft, blockBadgeTop) = blockBadgeBounds(
                block, blockLaneIndex, state, componentWidth, blockBadgeSize, axisHeight, componentHeight, lanes.size
            )
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(blockBadgeLeft.roundToInt(), blockBadgeTop.roundToInt()) }
                    .onSizeChanged(onBlockBadgeSizeChanged)
            ) {
                BlockBadge(
                    block = block,
                    style = selectionStyle,
                    onArrowClick = {
                        onShowBlockContextMenu(Offset(blockBadgeLeft, blockBadgeTop + blockBadgeSize.height))
                    },
                )
            }
        }

        // --- Menu kontekstowe selekcji obszaru ---
        Box(
            modifier = Modifier.absoluteOffset {
                IntOffset(contextMenuAnchorPx.x.roundToInt(), contextMenuAnchorPx.y.roundToInt())
            }
        ) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = onDismissContextMenu,
            ) {
                val currentSel = state.selection
                if (currentSel != null) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${formatTimeSeconds(currentSel.minSeconds)} – ${formatTimeSeconds(currentSel.maxSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = formatDuration(currentSel.durationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
                contextMenuContent()
            }
        }

        // --- Menu kontekstowe bloczku ---
        Box(
            modifier = Modifier.absoluteOffset {
                IntOffset(blockContextMenuAnchorPx.x.roundToInt(), blockContextMenuAnchorPx.y.roundToInt())
            }
        ) {
            DropdownMenu(
                expanded = showBlockContextMenu,
                onDismissRequest = onDismissBlockContextMenu,
            ) {
                val selectedBlock = state.selectedBlock
                if (selectedBlock != null) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        if (selectedBlock.label.isNotBlank()) {
                            Text(
                                text = selectedBlock.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(
                            text = "${formatTimeSeconds(selectedBlock.startSeconds)} – ${formatTimeSeconds(selectedBlock.endSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedBlock.label.isBlank()) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = formatDuration(selectedBlock.endSeconds - selectedBlock.startSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selectedBlock.description.isNotBlank()) {
                            Text(
                                text = selectedBlock.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider()
                }
                blockContextMenuContent(onDismissBlockContextMenu)
            }
        }
    }
}

@Composable
private fun SelectionBadge(
    selection: TimeSelection,
    style: TimelineTheme.SelectionStyle,
    onArrowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(style.badgeBgColor)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = formatDuration(selection.durationSeconds), style = style.badgeTextStyle)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onArrowClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "▾", style = style.badgeTextStyle.copy(color = style.badgeIconColor))
        }
    }
}

@Composable
private fun BlockBadge(
    block: TimelineBlock,
    style: TimelineTheme.SelectionStyle,
    onArrowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(style.badgeBgColor)
            .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val durationSeconds = block.endSeconds - block.startSeconds
        Text(
            text = if (block.label.isNotBlank()) block.label else formatDuration(durationSeconds),
            style = style.badgeTextStyle,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .clickable(onClick = onArrowClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "▾", style = style.badgeTextStyle.copy(color = style.badgeIconColor))
        }
    }
}

/**
 * Computes the top-left position of the selection badge in viewport pixels.
 */
private fun badgeBounds(
    selection: TimeSelection,
    state: TimelineState,
    componentWidth: Float,
    badgeSize: IntSize,
    axisHeight: Float,
): Pair<Float, Float> {
    val selMinVx = (selection.minSeconds * state.pixelsPerSecond - state.scrollOffset).coerceIn(0f, componentWidth)
    val selMaxVx = (selection.maxSeconds * state.pixelsPerSecond - state.scrollOffset).coerceIn(0f, componentWidth)
    val cx = (selMinVx + selMaxVx) / 2f
    val left = (cx - badgeSize.width / 2f).coerceIn(0f, (componentWidth - badgeSize.width).coerceAtLeast(0f))
    val top = axisHeight + 4f
    return Pair(left, top)
}

/**
 * Computes the top-left position of the block badge in viewport pixels.
 */
internal fun blockBadgeBounds(
    block: TimelineBlock,
    laneIndex: Int,
    state: TimelineState,
    componentWidth: Float,
    badgeSize: IntSize,
    axisHeight: Float,
    componentHeight: Float,
    laneCount: Int,
): Pair<Float, Float> {
    val lanesHeight = componentHeight - axisHeight
    val laneHeight = lanesHeight / laneCount.coerceAtLeast(1)
    val laneTop = axisHeight + laneIndex * laneHeight
    val blockMinVx = (block.startSeconds * state.pixelsPerSecond - state.scrollOffset).coerceIn(0f, componentWidth)
    val blockMaxVx = (block.endSeconds * state.pixelsPerSecond - state.scrollOffset).coerceIn(0f, componentWidth)
    val cx = (blockMinVx + blockMaxVx) / 2f
    val left = (cx - badgeSize.width / 2f).coerceIn(0f, (componentWidth - badgeSize.width).coerceAtLeast(0f))
    val top = laneTop + 4f
    return Pair(left, top)
}
