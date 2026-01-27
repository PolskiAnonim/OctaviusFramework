package org.octavius.feature.books.form.book

import kotlinx.coroutines.launch
import org.octavius.dialog.DialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.feature.books.domain.ReadingStatus
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.ComparisonType
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.DependencyType
import org.octavius.form.control.base.RepeatableValidation
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.Tr

class BookFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineContentOrder(): List<String> = listOf("basicInfo", "authors")
    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton", "deleteButton")

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "id" to IntegerControl(null),

        // Podstawowe informacje
        "titlePl" to StringControl(
            Tr.Books.Form.titlePl(),
            required = true
        ),
        "titleEng" to StringControl(
            Tr.Books.Form.titleEng(),
            required = false
        ),
        "status" to EnumControl(
            Tr.Books.Form.status(),
            ReadingStatus::class,
            required = true
        ),
        "basicInfo" to SectionControl(
            controls = listOf("titlePl", "titleEng", "status"),
            collapsible = false,
            initiallyExpanded = true,
            columns = 1,
            label = Tr.Books.Form.basicInfo()
        ),

        // Autorzy
        "authors" to RepeatableControl(
            rowControls = mapOf(
                "authorId" to DatabaseControl(
                    label = Tr.Books.Form.author(),
                    relatedTable = "books.authors",
                    displayColumn = "name",
                    required = true
                )
            ),
            rowOrder = listOf("authorId"),
            label = Tr.Books.Form.authors(),
            validationOptions = RepeatableValidation(
                minItems = 0,
                maxItems = 10,
                uniqueFields = listOf("authorId")
            )
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
                            text = Tr.Books.Form.confirmDelete(),
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