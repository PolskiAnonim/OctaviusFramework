package org.octavius.asianMedia.form

import org.octavius.database.DatabaseManager
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormValidator
import org.octavius.localization.Translations

class AsianMediaValidator(private val entityId: Int? = null) : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        return validateTitleDuplication(formData) && validateTitlesAgainstDatabase(formData)
    }

    private fun validateTitleDuplication(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.value as List<String>
        val hasDuplicates = titles.size != titles.toSet().size

        if (hasDuplicates) {
            errorManager.addFieldError("titles", Translations.get("validation.duplicateTitles"))
        }

        return !hasDuplicates
    }

    private fun validateTitlesAgainstDatabase(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.value as List<String>

        if (titles.isEmpty()) return true


        val params = if (entityId != null) mapOf("titles" to titles.toTypedArray(), "id" to entityId) else mapOf("titles" to titles.toTypedArray())
        val count = DatabaseManager.
        getFetcher().
        fetchCount("SELECT id, UNNEST(titles) AS title FROM titles",
            "title = ANY(ARRAY[:titles]) ${if (entityId != null) "AND id != :id" else ""}", params)

        if (count > 0L) {
            errorManager.addGlobalError(Translations.get("validation.titlesAlreadyExist"))
        }

        return count == 0L
    }
}