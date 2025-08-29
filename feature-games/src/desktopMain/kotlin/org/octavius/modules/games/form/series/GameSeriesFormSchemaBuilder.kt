package org.octavius.modules.games.form.series

import org.octavius.data.contract.ColumnInfo
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.StringValidation
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.button.SubmitButtonControl
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.localization.Translations

class GameSeriesFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                "name" to StringControl(
                    ColumnInfo("series", "name"),
                    Translations.get("games.series.name"),
                    required = true
                ),
                "basicInfo" to SectionControl(
                    ctrls = listOf("name"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 1,
                    label = Translations.get("games.series.basicInfo")
                ),
                // Przyciski
                "saveButton" to SubmitButtonControl(
                    text = Translations.get("action.save"),
                    actionKey = "save",
                    validates = true,
                    buttonType = ButtonType.Filled
                ),
                "cancelButton" to SubmitButtonControl(
                    text = Translations.get("action.cancel"),
                    actionKey = "cancel",
                    validates = false,
                    buttonType = ButtonType.Outlined
                )
            ),
            listOf("basicInfo")
        )
    }
}