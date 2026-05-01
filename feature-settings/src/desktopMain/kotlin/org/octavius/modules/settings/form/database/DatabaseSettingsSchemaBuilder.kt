package org.octavius.modules.settings.form.database

import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.localization.Tr

class DatabaseSettingsSchemaBuilder : FormSchemaBuilder() {
    override fun defineControls(): Map<String, Control<*>> = mapOf(
        "url" to StringControl(
            label = Tr.Settings.Database.url(),
            required = true
        ),
        "username" to StringControl(
            label = Tr.Settings.Database.username(),
            required = true
        ),
        "password" to StringControl(
            label = Tr.Settings.Database.password(),
            required = true
        ),
        "save" to ButtonControl(
            text = Tr.Settings.Database.save(),
            buttonType = ButtonType.Filled,
            actions = listOf(ControlAction {
                trigger.triggerAction("save", validates = true)
            })
        )
    )

    override fun defineContentOrder(): List<String> = listOf("url", "username", "password")

    override fun defineActionBarOrder(): List<String> = listOf("save")
}
