package org.octavius.report.column

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.report.filter.Filter

abstract class ReportColumn(
    val fieldName: String,
    val header: String,
    val width: Float = 1f,
    val filterable: Boolean = false,
    val sortable: Boolean = false
) {
    val name: String get() = fieldName // Dla zachowania kompatybilności

    @Composable
    abstract fun RenderCell(item: Any?, modifier: Modifier)
    
    /**
     * Tworzy filtr dla tej kolumny (tylko jeśli filterable = true)
     */
    open fun createFilter(): Filter? = null
}