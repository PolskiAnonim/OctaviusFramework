package org.octavius.modules.games.form.series

import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.form.ControlResultData
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

    override fun processFormData(formData: Map<String, ControlResultData>, loadedId: Int?): List<DatabaseStep> {
        val seriesData = mutableMapOf<String, DatabaseValue>()
        seriesData["name"] = DatabaseValue.Value(formData["name"]!!.currentValue)

        return if (loadedId != null) {
            listOf(DatabaseStep.Update("series", seriesData, mapOf("id" to DatabaseValue.Value(loadedId))))
        } else {
            listOf(DatabaseStep.Insert("series", seriesData))
        }
    }
}