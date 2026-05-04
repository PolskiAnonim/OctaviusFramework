package org.octavius.modules.sandbox.form

import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.localization.Tr
import org.octavius.modules.sandbox.domain.SandboxPriority
import org.octavius.ui.snackbar.SnackbarManager

class SandboxFormDataManager : FormDataManager() {

    override fun initData(payload: Map<String, Any?>): Map<String, Any?> {
        return mapOf(
            "basicInfo" to mapOf(
                "name" to "",
                "quantity" to null,
                "active" to false,
                "priority" to SandboxPriority.Medium,
                "startDate" to null,
                "tags" to emptyList<String>()
            ),
            "elements" to emptyList<Map<String, Any?>>(),
            "nestedRepeatable" to emptyList<Map<String, Any?>>()
        ) + payload
    }

    override fun definedFormActions(): Map<String, (formResultData: FormResultData) -> FormActionResult> {
        return mapOf(
            "save" to { _ ->
                SnackbarManager.showMessage(Tr.Sandbox.Form.savedMessage())
                FormActionResult.CloseScreen
            },
            "cancel" to { _ ->
                FormActionResult.CloseScreen
            }
        )
    }
}
