package org.octavius.modules.activity.form.category

import org.octavius.data.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent

class CategoryDataManager : FormDataManager() {

    private fun loadCategoryData(loadedId: Int?) = loadData(loadedId) {
        from("activity_tracker.categories", "c")
        map("name")
        map("color")
        map("icon")
        map("parentId", "parent_id")
    }

    override fun initData(
        loadedId: Int?,
        payload: Map<String, Any?>
    ): Map<String, Any?> {
        val loadedData = loadCategoryData(loadedId)
        val defaults = mapOf("color" to "#6366F1")
        return defaults + loadedData + payload
    }

    override fun definedFormActions(): Map<String, (FormResultData, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val params = mapOf(
            "name" to formResultData.getCurrent("name"),
            "color" to formResultData.getCurrent("color"),
            "icon" to formResultData.getCurrent("icon"),
            "parent_id" to formResultData.getCurrent("parentId")
        )

        val result = if (loadedId != null) {
            dataAccess.update("activity_tracker.categories")
                .setValues(params)
                .where("id = :id")
                .execute(params + ("id" to loadedId))
        } else {
            dataAccess.insertInto("activity_tracker.categories")
                .values(params)
                .execute(params)
        }

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success -> FormActionResult.CloseScreen
        }
    }
}
