package org.octavius.modules.games.form.series

import org.octavius.data.contract.DataResult
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.data.contract.toDatabaseValue
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.component.TableRelation
import org.octavius.form.control.base.FormResultData
import org.octavius.localization.T

class GameSeriesFormDataManager : FormDataManager() {
    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("series") // Główna tabela
        )
    }

    override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
        return emptyMap() // Tylko nazwa, brak domyślnych wartości
    }

    override fun definedFormActions(): Map<String, (FormResultData, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen },
            "validate" to { formData, loadedId -> validateTitleAgainstDatabase(formData, loadedId) }
        )
    }

    private fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val seriesData = mutableMapOf<String, DatabaseValue>()
        seriesData["name"] = formResultData["name"]!!.currentValue.toDatabaseValue()

        val steps = if (loadedId != null) {
            listOf(DatabaseStep.Update("series", seriesData, mapOf("id" to loadedId.toDatabaseValue())))
        } else {
            listOf(DatabaseStep.Insert("series", seriesData))
        }
        val result = batchExecutor.execute(steps)
        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return FormActionResult.Failure
            }

            is DataResult.Success<*> -> return FormActionResult.CloseScreen
        }
    }

    private fun validateTitleAgainstDatabase(
        formResultData: FormResultData,
        loadedId: Int?
    ): FormActionResult {
        val name = formResultData["name"]!!.currentValue as String

        // Sprawdź unikalność nazwy serii
        val existingCount = dataFetcher.query().from("series").where("name = :name").toCount(mapOf("name" to name))

        when (existingCount) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(existingCount.error))
                return FormActionResult.Failure
            }

            is DataResult.Success<Long> -> {
                val count = existingCount.value
                if (count > 0L) {
                    errorManager.addFieldError("name", T.get("games.validation.duplicatedSeries"))
                    return FormActionResult.ValidationFailed
                } else {
                    return FormActionResult.Success
                }
            }
        }
    }
}