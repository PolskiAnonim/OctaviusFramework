package org.octavius.modules.games.form.series

import org.octavius.data.DataResult
import org.octavius.data.builder.execute
import org.octavius.data.transaction.TransactionPlan
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent

class GameSeriesFormDataManager : FormDataManager() {
    private fun loadData(loadedId: Int?) = loadData(loadedId) {
        from("games.series", "s")
        map("name")
    }

    override fun initData(
        loadedId: Int?,
        payload: Map<String, Any?>
    ): Map<String, Any?> {
        val loadedData = loadData(loadedId)
        return loadedData + payload
    }

    override fun definedFormActions(): Map<String, (FormResultData, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val seriesData = mutableMapOf<String, Any?>()
        seriesData["name"] = formResultData.getCurrent("name")
        val plan = TransactionPlan()
        if (loadedId != null) {
            plan.add(
                dataAccess.update("games.series").setValues(listOf("name")).where("id = :id").asStep()
                    .execute("name" to formResultData.getCurrent("name"), "id" to loadedId)
            )
        } else {
            plan.add(
                dataAccess.insertInto("games.series").values(listOf("name")).asStep().execute("name" to formResultData.getCurrent("name"))
            )
        }
        return when (val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }

            is DataResult.Success<*> -> FormActionResult.CloseScreen
        }
    }
}