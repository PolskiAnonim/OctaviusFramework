package org.octavius.novels.form

import androidx.compose.runtime.Composable

abstract class Form {
    val formSchema: FormControls by lazy {
        createSchema()
    }

    protected abstract fun createSchema(): FormControls

    protected abstract fun initialize()

    @Composable
    fun display() {
        for (control in formSchema.order) {
            formSchema.controls[control]?.display(formSchema.controls)
        }
    }
}