package org.octavius.modules.games.form.series

import org.octavius.database.SaveOperation
import org.octavius.database.TableRelation
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormDataManager

class GameSeriesFormDataManager : FormDataManager() {
    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("series") // Główna tabela
        )
    }

    override fun initData(loadedId: Int?): Map<String, Any?> {
        return emptyMap() // Tylko nazwa, brak domyślnych wartości
    }

    override fun processFormData(formData: Map<String, ControlResultData>, loadedId: Int?): List<SaveOperation> {
        val seriesData = mutableMapOf<String, Any?>()
        seriesData["name"] = formData["name"]!!.currentValue

        return if (loadedId != null) {
            listOf(SaveOperation.Update("series", seriesData, loadedId))
        } else {
            listOf(SaveOperation.Insert("series", seriesData))
        }
    }
}