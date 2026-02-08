package org.octavius.modules.activity.form.rule

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.BooleanControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.Tr
import org.octavius.modules.activity.domain.MatchType

class RuleSchemaBuilder : FormSchemaBuilder() {

    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")
    override fun defineContentOrder(): List<String> = listOf("categoryId", "matchType", "pattern", "priority", "isActive")

    override fun defineControls(): Map<String, Control<*>> =
        mapOf(
            "id" to IntegerControl(null),
            "categoryId" to DatabaseControl(
                label = Tr.ActivityTracker.Rule.category(),
                relatedTable = "activity_tracker.categories",
                displayColumn = "name",
                required = true
            ),
            "matchType" to EnumControl(
                label = Tr.ActivityTracker.Rule.matchType(),
                enumClass = MatchType::class,
                required = true
            ),
            "pattern" to StringControl(
                Tr.ActivityTracker.Rule.pattern(),
                required = true
            ),
            "priority" to IntegerControl(
                Tr.ActivityTracker.Rule.priority(),
                required = true
            ),
            "isActive" to BooleanControl(
                Tr.ActivityTracker.Rule.isActive()
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
