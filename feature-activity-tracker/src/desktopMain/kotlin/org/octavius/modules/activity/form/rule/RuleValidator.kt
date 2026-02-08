package org.octavius.modules.activity.form.rule

import org.octavius.data.DataResult
import org.octavius.data.builder.toField
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.base.getCurrentAs
import org.octavius.localization.Tr
import org.octavius.modules.activity.domain.MatchType

class RuleValidator : FormValidator() {

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> = mapOf(
        "save" to { result -> checkIfUnique(result) }
    )

    private fun checkIfUnique(result: FormResultData): Boolean {
        val pattern = result.getCurrentAs<String>("pattern")
        val matchType = result.getCurrentAs<MatchType>("matchType")
        val id = result.getCurrent("id")
        val params = mutableMapOf<String, Any?>(
            "pattern" to pattern,
            "match_type" to matchType
        )
        val builder = dataAccess.select("COUNT(*)")
            .from("activity_tracker.categorization_rules")

        if (id != null) {
            builder.where("id != :id AND match_type = :match_type AND pattern = :pattern")
            params["id"] = id
        } else {
            builder.where("match_type = :match_type AND pattern = :pattern")
        }

        return when (val queryResult = builder.toField<Long>(params)) {
            is DataResult.Success -> {
                if ((queryResult.value ?: 0L) > 0) {
                    errorManager.setFieldErrors("pattern", listOf(Tr.ActivityTracker.Validation.patternExists()))
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
