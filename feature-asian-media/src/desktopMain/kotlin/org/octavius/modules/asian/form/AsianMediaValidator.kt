package org.octavius.modules.asian.form

import org.octavius.data.contract.DataResult
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormValidator
import org.octavius.localization.Translations
import org.octavius.navigation.AppRouter
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class AsianMediaValidator(private val entityId: Int? = null) : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        return validateTitleDuplication(formData) && validateTitlesAgainstDatabase(formData)
    }

    private fun validateTitleDuplication(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.currentValue as List<String>
        val hasDuplicates = titles.size != titles.toSet().size

        if (hasDuplicates) {
            errorManager.addFieldError("titles", Translations.get("asianMedia.form.duplicateTitles"))
        }

        return !hasDuplicates
    }

    private fun validateTitlesAgainstDatabase(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.currentValue as List<String>

        if (titles.isEmpty()) return true


        val params = if (entityId != null) mapOf("titles" to titles, "id" to entityId) else mapOf("titles" to titles)
        val result = dataFetcher.query().from("SELECT id, UNNEST(titles) AS title FROM titles")
            .where("title = ANY(:titles) ${if (entityId != null) "AND id != :id" else ""}").toCount(params)


        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return false
            }
            is DataResult.Success<Long> -> {
                if (result.value > 0L) {
                    errorManager.addGlobalError(Translations.get("asianMedia.form.titlesAlreadyExist"))
                    return false
                } else {
                    return true
                }
            }
        }
    }
}