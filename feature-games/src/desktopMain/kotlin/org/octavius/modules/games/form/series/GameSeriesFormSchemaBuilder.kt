package org.octavius.modules.games.form.series

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.localization.Tr

class GameSeriesFormSchemaBuilder : FormSchemaBuilder() {
    override fun defineContentOrder(): List<String> = listOf("basicInfo")
    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")

    override fun defineControls(): Map<String, Control<*>> =
        mapOf(
            "name" to StringControl(
                Tr.Games.Series.name(),
                required = true
            ),
            "basicInfo" to SectionControl(
                controls = listOf("name"),
                collapsible = false,
                initiallyExpanded = true,
                columns = 1,
                label = Tr.Games.Series.basicInfo()
            ),
            // Przyciski
            "saveButton" to ButtonControl(
                text = Tr.Action.save(), actions = listOf(
                    ControlAction {
                        trigger.triggerAction("save", true)
                    }
                ), buttonType = ButtonType.Filled
            ),
            "cancelButton" to ButtonControl(
                text = Tr.Action.cancel(), buttonType = ButtonType.Outlined, actions = listOf(
                    ControlAction {
                        trigger.triggerAction("cancel", false)
                    }
                )
            )
        )
}