package org.octavius.novels.domain.game.form

import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormSchemaBuilder
import org.octavius.novels.form.control.type.container.SectionControl
import org.octavius.novels.form.control.type.primitive.TextControl

class GameSeriesFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                "name" to TextControl(
                    ColumnInfo("series", "name"),
                    "Nazwa serii",
                    required = true
                ),
                "basicInfo" to SectionControl(
                    ctrls = listOf("name"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 1,
                    label = "Informacje o serii"
                )
            ),
            listOf("basicInfo")
        )
    }
}