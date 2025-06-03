package org.octavius.novels.form.component

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation

/**
 * Klasa zarządzająca ładowaniem i przetwarzaniem danych formularza
 */
abstract class FormDataManager {
    abstract fun initData(loadedId: Int?): Map<String, Any?>
    abstract fun defineTableRelations(): List<TableRelation>
    abstract fun processFormData(
        formData: Map<String, ControlResultData>,
        loadedId: Int?
    ): List<SaveOperation>

    fun loadEntityData(id: Int): Map<ColumnInfo, Any?> {
        val tableRelations = defineTableRelations()
        return DatabaseManager.getEntityWithRelations(id, tableRelations)
    }
}