package org.octavius.novels.domain.game.form

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.component.FormValidator

class GameSeriesValidator : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        val name = formData["name"]?.value as? String
        if (!name.isNullOrBlank()) {
            // Sprawdź unikalność nazwy serii
            val existingCount = DatabaseManager.executeQuery(
                "SELECT COUNT(*) as count FROM series WHERE name = ?",
                listOf(name)
            ).firstOrNull()!![ColumnInfo("","count")] as Int
            println(existingCount)
            if (existingCount > 0) {
                errorManager.addFieldError("name", "Seria o tej nazwie już istnieje")
                return false
            }
        }
        
        return true
    }
}