package org.octavius.modules.asian.form

import org.octavius.data.DataResult
import org.octavius.data.QueryFragment
import org.octavius.data.builder.toField
import org.octavius.data.join
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrentAs
import org.octavius.localization.Tr

class AsianMediaValidator(private val entityId: Int? = null) : FormValidator() {
    override fun validateBusinessRules(formResultData: FormResultData): Boolean {
        return validateTitleDuplication(formResultData)
    }

    private fun validateTitleDuplication(formResultData: FormResultData): Boolean {
        val titles = formResultData.getCurrentAs<List<String>>("titles")
        val hasDuplicates = titles.size != titles.toSet().size

        if (hasDuplicates) {
            errorManager.addFieldError("titles", Tr.AsianMedia.Form.duplicateTitles())
        }

        return !hasDuplicates
    }

    fun validateTitlesAgainstDatabase(formResultData: FormResultData): Boolean {
        val titles = formResultData.getCurrentAs<List<String>>("titles")

        if (titles.isEmpty()) return true

        val whereClause = listOfNotNull(
            QueryFragment("title = ANY(:titles)", mapOf("titles" to titles)),
            entityId?.let { QueryFragment("id != :id", mapOf("id" to entityId)) }
        ).join(separator = " AND ")

        val result = dataAccess.select("COUNT(*)").from("(SELECT id, UNNEST(titles) AS title FROM titles)")
            .where(whereClause.sql).toField<Long>(whereClause.params)


        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
            is DataResult.Success<Long?> -> {
                if ((result.value ?: 0L) > 0L) {
                    errorManager.addGlobalError(Tr.AsianMedia.Form.titlesAlreadyExist())
                    false
                } else {
                    true
                }
            }
        }
    }

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> {
        return mapOf(
            "save" to { formData -> validateTitlesAgainstDatabase(formData) }
        )
    }
}