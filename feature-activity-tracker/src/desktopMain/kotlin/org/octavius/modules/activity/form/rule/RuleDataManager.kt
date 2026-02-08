package org.octavius.modules.activity.form.rule

import org.octavius.data.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.modules.activity.domain.MatchType

class RuleDataManager : FormDataManager() {

    private fun loadRuleData(loadedId: Int?) = loadData(loadedId) {
        from("activity_tracker.categorization_rules", "r")
        map("categoryId", "category_id")
        map("matchType", "match_type")
        map("pattern")
        map("priority")
        map("isActive", "is_active")
    }

    override fun initData(
        loadedId: Int?,
        payload: Map<String, Any?>
    ): Map<String, Any?> {
        val loadedData = loadRuleData(loadedId)
        val defaults = mapOf(
            "priority" to 100,
            "isActive" to true,
            "matchType" to MatchType.ProcessName
        )
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
            "category_id" to formResultData.getCurrent("categoryId"),
            "match_type" to formResultData.getCurrent("matchType"),
            "pattern" to formResultData.getCurrent("pattern"),
            "priority" to formResultData.getCurrent("priority"),
            "is_active" to formResultData.getCurrent("isActive")
        )

        val result = if (loadedId != null) {
            dataAccess.update("activity_tracker.categorization_rules")
                .setValues(params)
                .where("id = :id")
                .execute(params + ("id" to loadedId))
        } else {
            dataAccess.insertInto("activity_tracker.categorization_rules")
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
