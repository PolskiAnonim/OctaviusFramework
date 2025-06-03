package org.octavius.novels.form.component

import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control

class FormValidator {
    fun validateFields(controls: Map<String, Control<*>>, states: Map<String, ControlState<*>> ): Boolean {
        var isValid = true

        for ((controlName, control) in controls) {
            // Pobierz stan kontrolki
            val state = states[controlName]!!
            state.error.value = null
            control.validateControl(controlName, state, controls, states)

            if (state.error.value != null) {
                isValid = false
            }
        }

        return isValid
    }

    fun validateBusinessRules(formData: Map<String, ControlResultData>): Boolean {
        return true
    }

}