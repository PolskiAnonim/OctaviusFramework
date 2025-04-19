package org.octavius.novels.report.column

import androidx.compose.runtime.Composable

data class ColumnDefinition<T>(
    val id: String,                    // Unikalny identyfikator kolumny
    val header: String,                // Nagłówek kolumny
    val width: Float = 1f,             // Względna szerokość (1f = standardowa)
    val visible: Boolean = true,       // Czy kolumna jest domyślnie widoczna
    val sortable: Boolean = true,      // Czy można sortować po tej kolumnie
    val filterable: Boolean = true,    // Czy można filtrować po tej kolumnie
    val getValue: (T) -> Any?,         // Funkcja pobierająca wartość z obiektu
    val renderCell: @Composable (Any?) -> Unit,  // Renderowanie komórki
    val filterComponent: @Composable ((String) -> Unit) -> Unit
)