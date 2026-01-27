package org.octavius.modules.asian.form

import kotlinx.coroutines.launch
import org.octavius.dialog.DialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationStatus
import org.octavius.domain.asian.PublicationType
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.collection.StringListControl
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.BooleanControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.Tr

class AsianMediaFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineActionBarOrder(): List<String> = listOf("cancelButton", "saveButton", "deleteButton")
    override fun defineContentOrder(): List<String> = listOf("titleInfo", "publications")

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "id" to IntegerControl(
            null
        ),
        "titleInfo" to SectionControl(
            controls = listOf("titles", "language"),
            collapsible = false,
            initiallyExpanded = true,
            columns = 2,
            label = Tr.AsianMedia.Form.titleInfo()
        ),
        "titles" to StringListControl(
            Tr.AsianMedia.Form.titles(),
            required = true,
            validationOptions = StringListValidation(minItems = 1)
        ),
        "language" to EnumControl(
            Tr.AsianMedia.Form.originalLanguage(),
            PublicationLanguage::class,
            required = true
        ),

        // Sekcja publikacji - używa RepeatableControl
        "publications" to RepeatableControl(
            rowControls = createPublicationControls(),
            rowOrder = listOf(
                "publicationType",
                "status",
                "trackProgress",
                "volumes",
                "translatedVolumes",
                "chapters",
                "translatedChapters",
                "originalCompleted"
            ),
            validationOptions = RepeatableValidation(
                uniqueFields = listOf("publicationType"),
                minItems = 1,
                maxItems = 7
            ),
            label = Tr.AsianMedia.Form.publications()
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
                            text = "Czy potwierdzasz usunięcie", //TODO tłumaczenie
                            onDismiss = { GlobalDialogManager.dismiss() },
                            confirmButtonText = "Tak", //TODO tłumaczenie
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


    private fun createPublicationControls(): Map<String, Control<*>> {
        return mapOf(
            "id" to IntegerControl(null),
            "publicationType" to EnumControl(
                Tr.AsianMedia.Form.publicationType(),
                PublicationType::class,
                required = true
            ),
            "status" to EnumControl(
                Tr.AsianMedia.Form.readingStatus(),
                PublicationStatus::class,
                required = true,
                actions = listOf(
                    ControlAction {
                        val shouldBeChecked = (sourceValue in listOf(
                            PublicationStatus.PlanToRead, PublicationStatus.Reading,
                            PublicationStatus.Completed
                        ))
                        updateLocalControl("trackProgress", shouldBeChecked)
                    }
                )
            ),
            "trackProgress" to BooleanControl(
                Tr.AsianMedia.Form.trackProgress(),
                required = true
            ),
            "volumes" to IntegerControl(
                Tr.AsianMedia.Form.volumeCount(),
                dependencies = visibleWhenTrackProgress()
            ),
            "translatedVolumes" to IntegerControl(
                Tr.AsianMedia.Form.translatedVolumes(),
                dependencies = visibleWhenTrackProgress()
            ),
            "chapters" to IntegerControl(
                Tr.AsianMedia.Form.chapterCount(),
                dependencies = visibleWhenTrackProgress()
            ),
            "translatedChapters" to IntegerControl(
                Tr.AsianMedia.Form.translatedChapters(),
                dependencies = visibleWhenTrackProgress()
            ),
            "originalCompleted" to BooleanControl(
                Tr.AsianMedia.Form.originalCompleted(),
                required = true,
                dependencies = visibleWhenTrackProgress()
            )
        )
    }

    private fun visibleWhenTrackProgress(): Map<String, ControlDependency<Boolean>> = mapOf(
        "visible" to ControlDependency(
            controlName = "trackProgress",
            value = true,
            dependencyType = DependencyType.Visible,
            comparisonType = ComparisonType.Equals,
            scope = DependencyScope.Local
        )
    )

}