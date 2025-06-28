package org.octavius.modules.games.form.game

import org.octavius.form.component.FormDataManager
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormSchema

class GameFormHandler(entityId: Int?) : FormHandler(entityId) {
    override fun createFormSchema(): FormSchema {
        return GameFormSchemaBuilder().build()
    }

    override fun createDataManager(): FormDataManager {
        return GameFormDataManager()
    }
}