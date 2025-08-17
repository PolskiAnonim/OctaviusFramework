package org.octavius.modules.asian.form

import org.octavius.data.contract.ColumnInfo
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationStatus
import org.octavius.domain.asian.PublicationType
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.collection.StringListControl
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.BooleanControl
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.Translations

class AsianMediaFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                "titleInfo" to SectionControl(
                    ctrls = listOf("titles", "language"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 2,
                    label = Translations.get("asianMedia.form.titleInfo")
                ),
                "titles" to StringListControl(
                    ColumnInfo("titles", "titles"),
                    Translations.get("asianMedia.form.titles"),
                    required = true,
                    validationOptions = StringListValidation(minItems = 1)
                ),
                "language" to EnumControl(
                    ColumnInfo("titles", "language"),
                    Translations.get("asianMedia.form.originalLanguage"),
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
                    label = Translations.get("asianMedia.form.publications")
                )
            ),
            listOf("titleInfo", "publications")
        )
    }

    private fun createPublicationControls(): Map<String, Control<*>> {
        return mapOf(
            "id" to IntegerControl(null, null),
            "publicationType" to EnumControl(
                null,
                Translations.get("asianMedia.form.publicationType"),
                PublicationType::class,
                required = true
            ),
            "status" to EnumControl(
                null,
                Translations.get("asianMedia.form.readingStatus"),
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
                Translations.get("asianMedia.form.trackProgress"),
                required = true
            ),
            "volumes" to IntegerControl(
                null,
                Translations.get("asianMedia.form.volumeCount"),
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
                    )
                )
            ),
            "translatedVolumes" to IntegerControl(
                null,
                Translations.get("asianMedia.form.translatedVolumes"),
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
                    )
                )
            ),
            "chapters" to IntegerControl(
                null,
                Translations.get("asianMedia.form.chapterCount"),
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
                    )
                )
            ),
            "translatedChapters" to IntegerControl(
                null,
                Translations.get("asianMedia.form.translatedChapters"),
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
                    )
                )
            ),
            "originalCompleted" to BooleanControl(
                null,
                Translations.get("asianMedia.form.originalCompleted"),
                required = true,
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
                    )
                )
            )
        )
    }
}