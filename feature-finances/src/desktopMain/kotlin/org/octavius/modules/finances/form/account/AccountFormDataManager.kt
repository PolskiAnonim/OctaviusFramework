package org.octavius.modules.finances.form.account

import io.github.octaviusframework.db.api.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.base.getInitial

class AccountFormDataManager : FormDataManager() {

    private fun loadData(loadedId: Any?) = loadData(loadedId) {
        from("finances.accounts", "a")
        map("id")
        map("name")
        map("type")
        map("currency")
        map("parentId", "parent_id")
    }

    override fun initData(
        payload: Map<String, Any?>
    ): Map<String, Any?> {
        val id = payload["id"]
        val loadedData = loadData(id)
        
        val defaultData = mapOf("currency" to "PLN")
        
        return defaultData + loadedData + payload
    }

    override fun definedFormActions(): Map<String, (FormResultData) -> FormActionResult> {
        return mapOf(
            "save" to { formData -> processSave(formData) },
            "cancel" to { _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData): FormActionResult {
        val loadedId = formResultData.getInitial("id")
        val params = mapOf(
            "name" to formResultData.getCurrent("name"),
            "type" to formResultData.getCurrent("type"),
            "currency" to formResultData.getCurrent("currency"),
            "parent_id" to formResultData.getCurrent("parentId")
        )

        val result = if (loadedId != null) {
            dataAccess.update("finances.accounts")
                .setValues(params)
                .where("id = @id")
                .execute(params + ("id" to loadedId))
        } else {
            dataAccess.insertInto("finances.accounts")
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
}
