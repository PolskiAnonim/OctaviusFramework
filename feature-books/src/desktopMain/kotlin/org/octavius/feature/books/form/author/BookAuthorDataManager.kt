package org.octavius.feature.books.form.author

import org.octavius.data.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent

class BookAuthorDataManager : FormDataManager() {

    private fun loadData(loadedId: Int?) = loadData(loadedId) {
        from("books.authors", "a")
        map("name")
        map("sortName")
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
            "delete" to { _, loadedId -> processDelete(loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val params = mapOf(
            "name" to formResultData.getCurrent("name"),
            "sort_name" to formResultData.getCurrent("sortName")
        )

        val result = if (loadedId != null) {
            dataAccess.update("books.authors")
                .setValues(params)
                .where("id = :id")
                .execute(params + ("id" to loadedId))
        } else {
            dataAccess.insertInto("books.authors")
                .values(params)
                .execute(params)
        }

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success<*> -> FormActionResult.CloseScreen
        }
    }

    private fun processDelete(loadedId: Int?): FormActionResult {
        if (loadedId == null) return FormActionResult.CloseScreen

        val result = dataAccess.deleteFrom("books.authors")
            .where("id = :id")
            .execute(mapOf("id" to loadedId))

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success<*> -> FormActionResult.CloseScreen
        }
    }
}