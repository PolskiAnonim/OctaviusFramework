package org.octavius.novels.domain.asian.form

import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.domain.PublicationType
import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormSchemaBuilder
import org.octavius.novels.form.control.base.ComparisonType
import org.octavius.novels.form.control.base.Control
import org.octavius.novels.form.control.base.ControlDependency
import org.octavius.novels.form.control.base.DependencyScope
import org.octavius.novels.form.control.base.DependencyType
import org.octavius.novels.form.control.type.*
import org.octavius.novels.form.control.base.RepeatableValidation
import org.octavius.novels.form.control.base.TextListValidation
import org.octavius.novels.form.control.type.collection.TextListControl
import org.octavius.novels.form.control.type.container.SectionControl
import org.octavius.novels.form.control.type.primitive.BooleanControl
import org.octavius.novels.form.control.type.primitive.HiddenControl
import org.octavius.novels.form.control.type.primitive.IntegerControl
import org.octavius.novels.form.control.type.repeatable.RepeatableControl
import org.octavius.novels.form.control.type.selection.EnumControl

class AsianMediaFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                "titleInfo" to SectionControl(
                    ctrls = listOf("titles", "language"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 2,
                    label = "Informacje o tytule"
                ),
                "titles" to TextListControl(
                    ColumnInfo("titles", "titles"),
                    "Tytuły",
                    required = true,
                    validationOptions = TextListValidation(minItems = 1)
                ),
                "language" to EnumControl(
                    ColumnInfo("titles", "language"),
                    "Język oryginału",
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
                    label = "Publikacje"
                )
            ),
            listOf("titleInfo", "publications")
        )
    }

    private fun createPublicationControls(): Map<String, Control<*>> {
        return mapOf(
            "id" to HiddenControl<Int>(null),
            "publicationType" to EnumControl(
                null,
                "Typ publikacji",
                PublicationType::class,
                required = true
            ),
            "status" to EnumControl(
                null,
                "Status czytania",
                PublicationStatus::class,
                required = true
            ),
            "trackProgress" to BooleanControl(
                null,
                "Śledzić postęp?",
                required = true
            ),
            "volumes" to IntegerControl(
                null,
                "Liczba tomów",
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
                "Przetłumaczone tomy",
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
                "Liczba rozdziałów",
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
                "Przetłumaczone rozdziały",
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
                "Oryginał ukończony",
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