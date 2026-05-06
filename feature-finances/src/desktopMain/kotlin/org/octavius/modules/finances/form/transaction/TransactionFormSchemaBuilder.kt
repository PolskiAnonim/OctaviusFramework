package org.octavius.modules.finances.form.transaction

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.datetime.InstantControl
import org.octavius.form.control.type.number.BigDecimalControl
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.localization.Tr
import java.math.BigDecimal

class TransactionFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineContentOrder(): List<String> = listOf("date", "description", "splits")
    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton")

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "id" to IntegerControl(null),
        "date" to InstantControl(
            Tr.Finances.Transaction.date(),
            required = true
        ),
        "description" to StringControl(
            Tr.Finances.Transaction.description(),
            required = true
        ),
        "splits" to RepeatableControl(
            rowControls = mapOf(
                "accountId" to DatabaseControl(
                    label = Tr.Finances.Transaction.account(),
                    relatedTable = "finances.accounts",
                    displayColumn = "name",
                    required = true
                ),
                "amount" to BigDecimalControl(
                    label = Tr.Finances.Transaction.amount(),
                    required = true,
                    validationOptions = BigDecimalValidation(
                        step = BigDecimal("0.01")
                    )
                )
            ),
            rowOrder = listOf("accountId", "amount"),
            label = Tr.Finances.Transaction.splits(),
            validationOptions = RepeatableValidation(
                minItems = 2,
                uniqueFields = listOf("accountId")
            )
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
