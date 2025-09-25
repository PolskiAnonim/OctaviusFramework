package org.octavius.modules.asian.form

import org.octavius.data.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.localization.T

class AsianMediaValidator(private val entityId: Int? = null) : FormValidator() {
    override fun validateBusinessRules(formResultData: FormResultData): Boolean {
        return validateTitleDuplication(formResultData)
    }

    private fun validateTitleDuplication(formResultData: FormResultData): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formResultData["titles"]!!.currentValue as List<String>
        val hasDuplicates = titles.size != titles.toSet().size

        if (hasDuplicates) {
            errorManager.addFieldError("titles", T.get("asianMedia.form.duplicateTitles"))
        }

        return !hasDuplicates
    }

    fun validateTitlesAgainstDatabase(formResultData: FormResultData): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formResultData["titles"]!!.currentValue as List<String>

        if (titles.isEmpty()) return true

        val params = if (entityId != null) mapOf("titles" to titles, "id" to entityId) else mapOf("titles" to titles)
        val result = dataAccess.select("COUNT(*)").from("(SELECT id, UNNEST(titles) AS title FROM titles)")
            .where("title = ANY(:titles) ${if (entityId != null) "AND id != :id" else ""}").toField<Long>(params)


        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return false
            }
            is DataResult.Success<Long?> -> {
                if ((result.value ?: 0L) > 0L) {
                    errorManager.addGlobalError(T.get("asianMedia.form.titlesAlreadyExist"))
                    return false
                } else {
                    return true
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