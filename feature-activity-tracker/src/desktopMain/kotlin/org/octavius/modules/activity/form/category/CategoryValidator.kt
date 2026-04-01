package org.octavius.modules.activity.form.category

import org.octavius.data.DataResult
import org.octavius.data.builder.toField
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.base.getCurrentAs
import org.octavius.localization.Tr

class CategoryValidator : FormValidator() {

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> = mapOf(
        "save" to { result -> checkIfUnique(result) }
    )

    private fun checkIfUnique(result: FormResultData): Boolean {
        val name = result.getCurrentAs<String>("name")
        val id = result.getCurrent("id")
        val params = mutableMapOf<String, Any?>("name" to name)
        val builder = dataAccess.select("COUNT(*)").from("activity_tracker.categories")
        if (id != null) {
            builder.where("id != :id AND name = :name")
            params["id"] = id
        } else {
            builder.where("name = :name")
        }
        return when (val queryResult = builder.toField<Long>(params)) {
            is DataResult.Success -> {
                if ((queryResult.value ?: 0L) > 0) {
                    errorManager.setFieldErrors("name", listOf(Tr.ActivityTracker.Validation.categoryNameExists()))
                    false
                } else {
                    true
                }
            }
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(queryResult.error))
                false
            }
        }
    }
}
