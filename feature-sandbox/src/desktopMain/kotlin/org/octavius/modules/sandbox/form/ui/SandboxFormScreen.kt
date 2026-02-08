package org.octavius.modules.sandbox.form.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.modules.sandbox.form.SandboxFormDataManager
import org.octavius.modules.sandbox.form.SandboxFormSchemaBuilder

class SandboxFormScreen {
    companion object {
        fun create(): FormScreen {
            val title = Tr.Sandbox.Form.newItem()

            val formHandler = FormHandler(
                entityId = null,
                formSchemaBuilder = SandboxFormSchemaBuilder(),
                formDataManager = SandboxFormDataManager()
            )

            return FormScreen(title, formHandler)
        }
    }
}
