package org.octavius.games.form

import org.octavius.form.ControlResultData
import org.octavius.form.SaveOperation
import org.octavius.form.TableRelation
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
        val seriesData = mutableMapOf<String, ControlResultData>()
        seriesData["name"] = formData["name"]!!

        return if (loadedId != null) {
            listOf(SaveOperation.Update("series", seriesData, loadedId))
        } else {
            listOf(SaveOperation.Insert("series", seriesData))
        }
    }
}