package org.octavius.novels.domain.asian.form

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.component.FormValidator

class AsianMediaValidator(private val entityId: Int? = null) : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        return validateTitleDuplication(formData) && validateTitlesAgainstDatabase(formData)
    }

    private fun validateTitleDuplication(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.value as List<String>
        return titles.size == titles.toSet().size
    }

    private fun validateTitlesAgainstDatabase(formData: Map<String, ControlResultData>): Boolean {
        @Suppress("UNCHECKED_CAST")
        val titles = formData["titles"]!!.value as List<String>

        if (titles.isEmpty()) return true
        val placeholders = titles.joinToString { "?" }
        val sql = """
            SELECT COUNT(*) FROM (
                SELECT UNNEST(titles) AS title
                FROM titles
            ) WHERE title = ANY(ARRAY[$placeholders])
            ${if (entityId != null) "AND id != ?" else ""}
        """.trimIndent()
        val count = DatabaseManager.executeQuery(sql, titles + if (entityId != null) listOf(entityId) else listOf())
            .firstOrNull()
            ?.values
            ?.firstOrNull() as Int

        return count == 0
    }
}