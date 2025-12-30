package org.octavius.modules.games.form.category

import org.octavius.data.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent

class GameCategoryDataManager : FormDataManager() {

    private fun loadData(loadedId: Int?) = loadData(loadedId) {
        from("games.categories", "ctg")
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
        val result = if (loadedId != null) {
            dataAccess.update("games.categories").setValue("name").where("id = :id")
                .execute(mapOf("name" to formResultData.getCurrent("name"), "id" to loadedId))
        } else {
            dataAccess.insertInto("games.categories").value("name")
                .execute(mapOf("name" to formResultData.getCurrent("name")))
        }
        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }

            is DataResult.Success<*> -> FormActionResult.CloseScreen
        }
    }
}