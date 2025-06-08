package org.octavius.novels.domain.game.form

import org.octavius.novels.form.component.*

class GameSeriesFormHandler(entityId: Int? = null) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema = GameSeriesFormSchemaBuilder().build()
    override fun createDataManager(): FormDataManager = GameSeriesFormDataManager()
    override fun createFormValidator(): FormValidator = GameSeriesValidator()
}