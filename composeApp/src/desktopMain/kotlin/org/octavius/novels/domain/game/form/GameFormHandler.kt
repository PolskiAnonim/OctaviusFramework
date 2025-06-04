package org.octavius.novels.domain.game.form

import org.octavius.novels.form.component.FormDataManager
import org.octavius.novels.form.component.FormHandler
import org.octavius.novels.form.component.FormSchema

class GameFormHandler(entityId: Int?) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema {
        return GameFormSchemaBuilder().build()
    }

    override fun createDataManager(): FormDataManager {
        return GameFormDataManager()
    }
}