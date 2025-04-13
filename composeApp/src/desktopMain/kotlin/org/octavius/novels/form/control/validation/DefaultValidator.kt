package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState

class DefaultValidator<T: Any> : ControlValidator<T>() {
    override fun validateSpecific(state: ControlState<*>) {
        // Domyślna implementacja nie wykonuje dodatkowej walidacji
    }
}