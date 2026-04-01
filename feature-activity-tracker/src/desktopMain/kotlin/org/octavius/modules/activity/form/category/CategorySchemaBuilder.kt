package org.octavius.modules.activity.form.category

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.localization.Tr

class CategorySchemaBuilder : FormSchemaBuilder() {

    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")
    override fun defineContentOrder(): List<String> = listOf("name", "color", "icon", "parentId")

    override fun defineControls(): Map<String, Control<*>> =
        mapOf(
            "id" to IntegerControl(null),
            "name" to StringControl(
                Tr.ActivityTracker.Category.name(),
                required = true
            ),
            "color" to StringControl(
                Tr.ActivityTracker.Category.color(),
                required = true
            ),
            "icon" to StringControl(
                Tr.ActivityTracker.Category.icon(),
                required = false
            ),
            "parentId" to DatabaseControl(
                label = Tr.ActivityTracker.Category.parentCategory(),
                relatedTable = "activity_tracker.categories",
                displayColumn = "name",
                required = false
            ),
            // Przyciski
            "saveButton" to ButtonControl(
                text = Tr.Action.save(),
                actions = listOf(
                    ControlAction {
                        trigger.triggerAction("save", true)
                    }
                ),
                buttonType = ButtonType.Filled
            ),
            "cancelButton" to ButtonControl(
                text = Tr.Action.cancel(),
                buttonType = ButtonType.Outlined,
                actions = listOf(
                    ControlAction {
                        trigger.triggerAction("cancel", false)
                    }
                )
            )
        )
}
