package org.octavius.novels.form

import androidx.compose.runtime.Composable
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.util.Converters.camelToSnakeCase

abstract class Form {
    private val formSchema: FormControls by lazy {
        createSchema()
    }

    protected abstract fun createSchema(): FormControls

    // Metoda do definiowania relacji tabel
    protected abstract fun defineTableRelations(): List<TableRelation>

    fun loadData(id: Int, databaseManager: DatabaseManager) {
        // Pobierz definicje relacji tabel
        val tableRelations = defineTableRelations()

        // Pobierz dane z bazy
        val data = databaseManager.getEntityWithRelations(id, tableRelations)

        // Mapuj dane na kontrolki
        for ((_, control) in formSchema.controls) {
            if (control.fieldName != null && control.tableName != null) {
                // Konwertuj nazwę kolumny z camelCase na snake_case jeśli potrzebne
                val columnName = camelToSnakeCase(control.fieldName)
                val tableName = camelToSnakeCase(control.tableName)
                val value = data[ColumnInfo(tableName, columnName)]

                // Ustaw wartość kontrolki (z odpowiednią konwersją typu)
                control.setInitValue(value)
            }
        }
    }

    @Composable
    fun display() {
        for (controlName in formSchema.order) {
            formSchema.controls[controlName]?.render(formSchema.controls)
        }
    }
}