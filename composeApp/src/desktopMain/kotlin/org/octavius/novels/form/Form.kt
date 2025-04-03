package org.octavius.novels.form

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.control.ControlState
import org.octavius.novels.util.Converters.camelToSnakeCase

abstract class Form {
    protected val formSchema: FormControls by lazy {
        createSchema()
    }

    protected abstract fun createSchema(): FormControls

    protected val formState: MutableMap<String, ControlState<*>> = mutableMapOf()
    // Metoda do definiowania relacji tabel
    protected abstract fun defineTableRelations(): List<TableRelation>

    fun loadData(id: Int) {
        // Pobierz definicje relacji tabel
        val tableRelations = defineTableRelations()

        // Pobierz dane z bazy
        val data = DatabaseManager.getEntityWithRelations(id, tableRelations)

        // Mapuj dane na kontrolki
        for ((controlName, control) in formSchema.controls) {
            if (control.fieldName != null && control.tableName != null) {
                // Konwertuj nazwę kolumny z camelCase na snake_case jeśli potrzebne
                val columnName = camelToSnakeCase(control.fieldName)
                val tableName = camelToSnakeCase(control.tableName)
                val value = data[ColumnInfo(tableName, columnName)]

                // Ustaw wartość kontrolki (z odpowiednią konwersją typu)
                formState[controlName] = control.setInitValue(value)
            }
        }
    }

    @Composable
    fun display() {
        LazyColumn {
            items(formSchema.order) {
                formSchema.controls[it]?.render(it, formSchema.controls, formState)
            }
        }
    }
}