package org.octavius.modules.games.form.series

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.localization.T

class GameSeriesFormSchemaBuilder : FormSchemaBuilder() {
    override fun defineContentOrder(): List<String> = listOf("basicInfo")
    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")

    override fun defineControls(): Map<String, Control<*>> =
        mapOf(
            "name" to StringControl(
                T.get("games.series.name"),
                required = true
            ),
            "basicInfo" to SectionControl(
                controls = listOf("name"),
                collapsible = false,
                initiallyExpanded = true,
                columns = 1,
                label = T.get("games.series.basicInfo")
            ),
            // Przyciski
            "saveButton" to ButtonControl(
                text = T.get("action.save"), actions = listOf(
                    ControlAction {
                        trigger.triggerAction("save", true)
                    }
                ), buttonType = ButtonType.Filled
            ),
            "cancelButton" to ButtonControl(
                text = T.get("action.cancel"), buttonType = ButtonType.Outlined, actions = listOf(
                    ControlAction {
                        trigger.triggerAction("cancel", false)
                    }
                )
            )
        )
}