package org.octavius.modules.sandbox.form

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.collection.StringListControl
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.datetime.DateControl
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.BooleanControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.Tr
import org.octavius.modules.sandbox.domain.SandboxPriority

class SandboxFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "name" to StringControl(
            Tr.Sandbox.Form.name(),
            required = true
        ),
        "quantity" to IntegerControl(
            Tr.Sandbox.Form.quantity(),
            validationOptions = IntegerValidation(min = 0)
        ),
        "active" to BooleanControl(
            Tr.Sandbox.Form.active(),
            required = true
        ),
        "priority" to EnumControl(
            Tr.Sandbox.Form.priority(),
            SandboxPriority::class,
            required = true
        ),
        "startDate" to DateControl(
            Tr.Sandbox.Form.startDate()
        ),
        "tags" to StringListControl(
            Tr.Sandbox.Form.tags()
        ),
        "basicInfo" to SectionControl(
            controls = listOf("name", "quantity", "active", "priority", "startDate", "tags"),
            collapsible = true,
            initiallyExpanded = true,
            columns = 2,
            label = Tr.Sandbox.Form.basicInfo()
        ),
        "elements" to RepeatableControl(
            rowControls = mapOf(
                "elementName" to StringControl(
                    Tr.Sandbox.Form.elementName(),
                    required = true
                ),
                "elementValue" to IntegerControl(
                    Tr.Sandbox.Form.elementValue(),
                    validationOptions = IntegerValidation(min = 0)
                )
            ),
            rowOrder = listOf("elementName", "elementValue"),
            label = Tr.Sandbox.Form.elements(),
            validationOptions = RepeatableValidation(
                minItems = 0,
                maxItems = 10,
                uniqueFields = listOf("elementName")
            )
        ),
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
            actions = listOf(
                ControlAction {
                    trigger.triggerAction("cancel", false)
                }
            ),
            buttonType = ButtonType.Outlined
        )
    )

    override fun defineContentOrder(): List<String> = listOf(
        "basicInfo",
        "elements"
    )

    override fun defineActionBarOrder(): List<String> = listOf(
        "cancelButton",
        "saveButton"
    )
}
