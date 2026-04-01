package org.octavius.modules.activity.form.rule.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.modules.activity.form.rule.RuleDataManager
import org.octavius.modules.activity.form.rule.RuleSchemaBuilder
import org.octavius.modules.activity.form.rule.RuleValidator

class RuleFormScreen {

    companion object {
        fun create(entityId: Int? = null): FormScreen {
            val title = if (entityId == null)
                Tr.ActivityTracker.Form.newRule()
            else
                Tr.ActivityTracker.Form.editRule()

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = RuleSchemaBuilder(),
                formDataManager = RuleDataManager(),
                formValidator = RuleValidator()
            )
            return FormScreen(title, formHandler)
        }
    }
}
