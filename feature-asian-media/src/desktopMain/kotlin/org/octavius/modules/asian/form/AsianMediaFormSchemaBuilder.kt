package org.octavius.modules.asian.form

import org.octavius.data.ColumnInfo
import org.octavius.dialog.DialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationStatus
import org.octavius.domain.asian.PublicationType
import org.octavius.form.component.FormSchema
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
import org.octavius.localization.T

class AsianMediaFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                "id" to IntegerControl(
                    ColumnInfo("titles", "id"),
                    null
                ),
                "titleInfo" to SectionControl(
                    ctrls = listOf("titles", "language"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 2,
                    label = T.get("asianMedia.form.titleInfo")
                ),
                "titles" to StringListControl(
                    ColumnInfo("titles", "titles"),
                    T.get("asianMedia.form.titles"),
                    required = true,
                    validationOptions = StringListValidation(minItems = 1)
                ),
                "language" to EnumControl(
                    ColumnInfo("titles", "language"),
                    T.get("asianMedia.form.originalLanguage"),
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
                    label = T.get("asianMedia.form.publications")
                ),
                // Przyciski
                "saveButton" to ButtonControl(
                    text = T.get("action.save"),
                    buttonType = ButtonType.Filled,
                    actions = listOf(
                        ControlAction {
                            trigger.triggerAction("save", true)
                        }
                    )
                ),
                "deleteButton" to ButtonControl(
                    text = T.get("action.remove"),
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
                                    title = T.get("action.confirm"),
                                    text = "Czy potwierdzasz usunięcie", //TODO tłumaczenie
                                    onDismiss = { GlobalDialogManager.dismiss() },
                                    confirmButtonText = "Tak", //TODO tłumaczenie
                                    onConfirm = {
                                        trigger.triggerAction("delete", false)
                                        GlobalDialogManager.dismiss()
                                    }
                                )
                            )
                        }
                    )
                ),
                "cancelButton" to ButtonControl(
                    text = T.get("action.cancel"),
                    buttonType = ButtonType.Outlined,
                    actions = listOf(
                        ControlAction {
                            trigger.triggerAction("cancel", false)
                        }
                    )
                )
            ),
            listOf("titleInfo", "publications"),
            listOf("cancelButton", "saveButton", "deleteButton")
        )
    }

    private fun createPublicationControls(): Map<String, Control<*>> {
        return mapOf(
            "id" to IntegerControl(null, null),
            "publicationType" to EnumControl(
                null,
                T.get("asianMedia.form.publicationType"),
                PublicationType::class,
                required = true
            ),
            "status" to EnumControl(
                null,
                T.get("asianMedia.form.readingStatus"),
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
                null,
                T.get("asianMedia.form.trackProgress"),
                required = true
            ),
            "volumes" to IntegerControl(
                null,
                T.get("asianMedia.form.volumeCount"),
                dependencies = visibleWhenTrackProgress()
            ),
            "translatedVolumes" to IntegerControl(
                null,
                T.get("asianMedia.form.translatedVolumes"),
                dependencies = visibleWhenTrackProgress()
            ),
            "chapters" to IntegerControl(
                null,
                T.get("asianMedia.form.chapterCount"),
                dependencies = visibleWhenTrackProgress()
            ),
            "translatedChapters" to IntegerControl(
                null,
                T.get("asianMedia.form.translatedChapters"),
                dependencies = visibleWhenTrackProgress()
            ),
            "originalCompleted" to BooleanControl(
                null,
                T.get("asianMedia.form.originalCompleted"),
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