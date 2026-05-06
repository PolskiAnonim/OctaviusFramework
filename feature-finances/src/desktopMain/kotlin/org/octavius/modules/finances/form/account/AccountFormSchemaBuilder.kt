package org.octavius.modules.finances.form.account

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.localization.Tr
import org.octavius.modules.finances.domain.AccountType

class AccountFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineContentOrder(): List<String> = listOf("name", "type", "currency", "parentId")
    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "id" to IntegerControl(null),
        "name" to StringControl(
            Tr.Finances.Account.name(),
            required = true
        ),
        "type" to EnumControl(
            Tr.Finances.Account.type(),
            AccountType::class,
            required = true
        ),
        "currency" to StringControl(
            Tr.Finances.Account.currency(),
            required = true
        ).apply { 
            // Default value should be handled in DataManager or here
        },
        "parentId" to DatabaseControl(
            Tr.Finances.Account.parent(),
            "finances.accounts",
            "name",
            required = false
        ),
        "saveButton" to ButtonControl(
            text = Tr.Action.save(),
            buttonType = ButtonType.Filled,
            actions = listOf(
                ControlAction {
                    trigger.triggerAction("save", true)
                }
            )
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
