package org.octavius.form.control.validator.section

import org.octavius.form.control.base.ControlContext
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.ControlValidator

class SectionValidator(
    private val childControlNames: List<String>
) : ControlValidator<Unit>() {

    override fun validateSpecific(controlContext: ControlContext, state: ControlState<*>) {
        // Główna odpowiedzialność walidatora sekcji:
        // uruchomić walidację dla każdej zdefiniowanej w niej kontrolki-dziecka.
        childControlNames.forEach { childName ->
            val childControl = formSchema.getControl(childName) ?: return@forEach
            val childState = formState.getControlState(childName) ?: return@forEach

            // Tworzymy kontekst dla dziecka. Rodzicem jest sekcja.
            val childContext = controlContext.forSectionChild(childName)

            // Delegujemy walidację
            childControl.validateControl(childContext, childState)
        }
    }
}