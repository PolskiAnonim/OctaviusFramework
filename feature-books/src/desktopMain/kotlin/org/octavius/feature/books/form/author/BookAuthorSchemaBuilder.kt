package org.octavius.feature.books.form.author

import kotlinx.coroutines.launch
import org.octavius.dialog.DialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.ComparisonType
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.DependencyType
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.localization.Tr

class BookAuthorSchemaBuilder : FormSchemaBuilder() {

    override fun defineContentOrder(): List<String> = listOf("name", "sortName")
    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton", "deleteButton")

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "id" to IntegerControl(null),
        "name" to StringControl(
            Tr.Books.Authors.Form.name(),
            required = true
        ),
        "sortName" to StringControl(
            Tr.Books.Authors.Form.sortName(),
            required = false
        ),
        // Przyciski
        "saveButton" to ButtonControl(
            text = Tr.Action.save(),
            buttonType = ButtonType.Filled,
            actions = listOf(
                ControlAction {
                    trigger.triggerAction("save", true)
                }
            )
        ),
        "deleteButton" to ButtonControl(
            text = Tr.Action.remove(),
            buttonType = ButtonType.Filled,
            dependencies = mapOf(
                "visible" to ControlDependency(
                    controlName = "id",
                    value = null,
                    dependencyType = DependencyType.Visible,
                    comparisonType = ComparisonType.NotEquals
                )
            ),
            actions = listOf(
                ControlAction {
                    GlobalDialogManager.show(
                        DialogConfig(
                            title = Tr.Action.confirm(),
                            text = Tr.Books.Authors.Form.confirmDelete(),
                            onDismiss = { GlobalDialogManager.dismiss() },
                            confirmButtonText = Tr.Action.confirm(),
                            onConfirm = {
                                coroutineScope.launch {
                                    trigger.triggerAction("delete", false)
                                    GlobalDialogManager.dismiss()
                                }
                            }
                        )
                    )
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