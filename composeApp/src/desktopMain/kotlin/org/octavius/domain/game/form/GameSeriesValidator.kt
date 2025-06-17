package org.octavius.domain.game.form

import org.octavius.database.DatabaseManager
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormValidator

class GameSeriesValidator : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        val name = formData["name"]?.value as? String
        if (!name.isNullOrBlank()) {
            // Sprawdź unikalność nazwy serii
            val existingCount = DatabaseManager.getFetcher().fetchCount("series", "name = :name", mapOf("name" to name))

            println(existingCount)
            if (existingCount > 0L) {
                errorManager.addFieldError("name", "Seria o tej nazwie już istnieje")
                return false
            }
        }
        
        return true
    }
}