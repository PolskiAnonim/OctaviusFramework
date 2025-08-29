package org.octavius.modules.games.form.series

import org.octavius.data.contract.ColumnInfo
import org.octavius.form.FormActionResult
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
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
                "saveButton" to ButtonControl(
                    text = Translations.get("action.save"), actions = listOf(
                        ControlAction {
                            val res = trigger.triggerAction("validate", true)
                            when (res) {
                                is FormActionResult.CloseScreen,
                                is FormActionResult.Failure,
                                is FormActionResult.Navigate,
                                is FormActionResult.ValidationFailed -> Unit //Niejawna obsługa lub brak wystąpień
                                is FormActionResult.Success -> trigger.triggerAction("save", false)
                            }
                        }), buttonType = ButtonType.Filled
                ),
                "cancelButton" to ButtonControl(
                    text = Translations.get("action.cancel"), buttonType = ButtonType.Outlined, actions = listOf(
                        ControlAction {
                            trigger.triggerAction("cancel", false)
                        }
                    )
                )
            ),
            listOf("basicInfo"),
            listOf("cancelButton", "saveButton"))
    }
}