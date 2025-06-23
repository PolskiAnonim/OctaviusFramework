package org.octavius.domain.game.form

import org.octavius.database.DatabaseManager
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormValidator
import org.octavius.localization.Translations

class GameSeriesValidator : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        val name = formData["name"]?.value as? String
        if (!name.isNullOrBlank()) {
            // Sprawdź unikalność nazwy serii
            val existingCount = DatabaseManager.getFetcher().fetchCount("series", "name = :name", mapOf("name" to name))

            println(existingCount)
            if (existingCount > 0L) {
                errorManager.addFieldError("name", Translations.get("games.validation.duplicatedSeries"))
                return false
            }
        }
        
        return true
    }
}