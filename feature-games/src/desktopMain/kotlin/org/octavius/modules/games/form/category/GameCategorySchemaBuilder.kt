package org.octavius.modules.games.form.category

import org.octavius.data.ColumnInfo
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.localization.T

class GameCategorySchemaBuilder : FormSchemaBuilder() {

    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")
    override fun defineContentOrder(): List<String> = listOf("name")

    override fun defineControls(): Map<String, Control<*>> =
        mapOf(
            "id" to IntegerControl(
                ColumnInfo("categories", "id"),
                null
            ),
            "name" to StringControl(
                ColumnInfo("categories", "name"),
                T.get("games.categories.name"),
                required = true
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