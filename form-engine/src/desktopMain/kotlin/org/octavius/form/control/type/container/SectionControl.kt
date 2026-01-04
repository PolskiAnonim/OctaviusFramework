package org.octavius.form.control.type.container

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormActionTrigger
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState
import org.octavius.form.control.base.*
import org.octavius.form.control.layout.section.SectionContent
import org.octavius.form.control.layout.section.SectionHeader
import org.octavius.form.control.validator.section.SectionValidator
import org.octavius.ui.theme.FormSpacing

/**
 * Kontrolka do grupowania i organizacji innych kontrolek w sekcje.
 *
 * Renderuje grupę kontrolek w karcie z nagłówkiem. Obsługuje składanie/rozwijanie sekcji,
 * układanie kontrolek w kolumnach oraz zarządzanie relacjami rodzic-dziecko między kontrolkami.
 * Umożliwia logiczne organizowanie formularza w tematyczne sekcje.
 */
class SectionControl(
    val controls: List<String>,
    val collapsible: Boolean = true,
    val initiallyExpanded: Boolean = true,
    val columns: Int = 1,
    label: String,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Unit>(label, false, dependencies, hasStandardLayout = false) {

    override val validator: ControlValidator<Unit> = SectionValidator(controls)

    override fun initializeControlLifecycle(
        formState: FormState,
        formSchema: FormSchema,
        errorManager: ErrorManager,
        formActionTrigger: FormActionTrigger
    ) {
        super.initializeControlLifecycle(formState, formSchema, errorManager, formActionTrigger)
        // Kluczowy moment: informujemy dzieci, że ich walidacja nie jest niezależna.
        controls.forEach { controlName ->
            formSchema.getControl(controlName)?.hierarchyRole = ControlHierarchyRole.GROUPED_CHILD
        }
    }

    @Composable
    override fun Display(controlContext: ControlContext, controlState: ControlState<Unit>, isRequired: Boolean) {
        val isExpanded = remember { mutableStateOf(initiallyExpanded) }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FormSpacing.containerPaddingVertical),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    label = label,
                    collapsible = collapsible,
                    isExpanded = isExpanded.value,
                    onToggle = { isExpanded.value = !isExpanded.value }
                )

                AnimatedVisibility(visible = isExpanded.value) {
                    SectionContent(
                        controlContext = controlContext,
                        controlNames = controls,
                        columns = columns,
                        formSchema = this@SectionControl.formSchema,
                        formState = this@SectionControl.formState
                    )
                }
            }
        }
    }
}