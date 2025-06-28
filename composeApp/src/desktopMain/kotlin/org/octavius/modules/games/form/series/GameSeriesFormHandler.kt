package org.octavius.modules.games.form.series

import org.octavius.form.component.FormDataManager
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormValidator

class GameSeriesFormHandler(entityId: Int? = null) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema = GameSeriesFormSchemaBuilder().build()
    override fun createDataManager(): FormDataManager = GameSeriesFormDataManager()
    override fun createFormValidator(): FormValidator = GameSeriesFormValidator()
}