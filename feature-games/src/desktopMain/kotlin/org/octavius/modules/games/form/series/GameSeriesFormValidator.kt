package org.octavius.modules.games.form.series

import org.octavius.data.contract.DataResult
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormValidator
import org.octavius.localization.Translations

class GameSeriesFormValidator : FormValidator() {
    override fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        val name = formData["name"]!!.currentValue as String

        // Sprawdź unikalność nazwy serii
        val existingCount = dataFetcher.query().from("series").where("name = :name").toCount(mapOf("name" to name))

        when (existingCount) {
            is DataResult.Failure -> {
                errorManager.addGlobalError("Błąd walidacji")
                return false
            }

            is DataResult.Success<Long> -> {
                val count = existingCount.value
                if (count > 0L) {
                    errorManager.addFieldError("name", Translations.get("games.validation.duplicatedSeries"))
                    return false
                } else {
                    return true
                }
            }
        }
    }

}