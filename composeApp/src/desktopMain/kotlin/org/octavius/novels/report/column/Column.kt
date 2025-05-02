package org.octavius.novels.report.column

import androidx.compose.runtime.Composable

import androidx.compose.ui.Modifier

abstract class ReportColumn(
    val name: String,
    val header: String,
    val width: Float = 1f,
    val sortable: Boolean = false
) {
    @Composable
    abstract fun RenderCell(item: Map<String, Any?>, modifier: Modifier = Modifier)
}