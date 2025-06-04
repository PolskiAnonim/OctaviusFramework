package org.octavius.novels.screens

import org.octavius.novels.domain.asian.form.AsianMediaFormHandler
import org.octavius.novels.form.component.FormScreen

class AsianMediaFormScreen(entityId: Int? = null) : FormScreen() {
    override val formHandler = AsianMediaFormHandler(entityId)
}