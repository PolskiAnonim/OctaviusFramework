package org.octavius.modules.activity.form.category.ui

import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr
import org.octavius.modules.activity.form.category.CategoryDataManager
import org.octavius.modules.activity.form.category.CategorySchemaBuilder
import org.octavius.modules.activity.form.category.CategoryValidator

class CategoryFormScreen {

    companion object {
        fun create(entityId: Int? = null): FormScreen {
            val title = if (entityId == null)
                Tr.ActivityTracker.Form.newCategory()
            else
                Tr.ActivityTracker.Form.editCategory()

            val formHandler = FormHandler(
                entityId = entityId,
                formSchemaBuilder = CategorySchemaBuilder(),
                formDataManager = CategoryDataManager(),
                formValidator = CategoryValidator()
            )
            return FormScreen(title, formHandler)
        }
    }
}
