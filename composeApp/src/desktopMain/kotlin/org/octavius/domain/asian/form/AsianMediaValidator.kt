package org.octavius.domain.asian.form

import org.octavius.database.DatabaseManager
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormValidator

class AsianMediaValidator(private val entityId: Int? = null) : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        return validateTitleDuplication(formData) && validateTitlesAgainstDatabase(formData)
    }

    private fun validateTitleDuplication(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.value as List<String>
        val hasDuplicates = titles.size != titles.toSet().size

        if (hasDuplicates) {
            errorManager.addFieldError("titles", "Wykryto duplikaty w tytułach")
        }

        return !hasDuplicates
    }

    private fun validateTitlesAgainstDatabase(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.value as List<String>

        if (titles.isEmpty()) return true


        val params = if (entityId != null) mapOf("titles" to titles, "id" to entityId) else mapOf("titles" to titles.toTypedArray())
        val count = DatabaseManager.
        getFetcher().
        fetchCount("(SELECT id, UNNEST(titles) AS title FROM titles)",
            "title = ANY(:titles) ${if (entityId != null) "AND id != :id" else ""}", params)

        if (count > 0L) {
            errorManager.addGlobalError("Tytuły już istnieją w bazie danych")
        }

        return count == 0L
    }
}