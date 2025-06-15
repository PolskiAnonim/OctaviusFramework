package org.octavius.novels.domain.game.form

import org.octavius.novels.form.component.FormDataManager
import org.octavius.novels.form.component.FormHandler
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormValidator

class GameSeriesFormHandler(entityId: Int? = null) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema = GameSeriesFormSchemaBuilder().build()
    override fun createDataManager(): FormDataManager = GameSeriesFormDataManager()
    override fun createFormValidator(): FormValidator = GameSeriesValidator()
}