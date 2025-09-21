package org.octavius.modules.games.form.series

import org.octavius.data.contract.DataResult
import org.octavius.data.contract.transaction.TransactionPlan
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.component.TableRelation
import org.octavius.form.control.base.FormResultData

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
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val seriesData = mutableMapOf<String, Any?>()
        seriesData["name"] = formResultData["name"]!!.currentValue
        val plan = TransactionPlan(dataAccess)
        if (loadedId != null) {
            plan.update("series", seriesData, mapOf("id" to loadedId))
        } else {
            plan.insert("series", seriesData)
        }
        val result = dataAccess.executeTransactionPlan(plan.build())
        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return FormActionResult.Failure
            }

            is DataResult.Success<*> -> return FormActionResult.CloseScreen
        }
    }
}