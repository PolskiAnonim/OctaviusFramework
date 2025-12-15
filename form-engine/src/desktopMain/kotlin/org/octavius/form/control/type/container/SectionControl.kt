package org.octavius.form.control.type.container

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.RenderContext
import org.octavius.form.control.layout.section.SectionContent
import org.octavius.form.control.layout.section.SectionHeader
import org.octavius.localization.T
import org.octavius.ui.theme.FormSpacing

/**
 * Kontrolka do grupowania i organizacji innych kontrolek w sekcje.
 *
 * Renderuje grupę kontrolek w karcie z nagłówkiem. Obsługuje składanie/rozwijanie sekcji,
 * układanie kontrolek w kolumnach oraz zarządzanie relacjami rodzic-dziecko między kontrolkami.
 * Umożliwia logiczne organizowanie formularza w tematyczne sekcje.
 */
class SectionControl(
    val ctrls: List<String>,
    val collapsible: Boolean = true,
    val initiallyExpanded: Boolean = true,
    val columns: Int = 1,
    label: String,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Unit>(label, false, dependencies, hasStandardLayout = false) {

    override fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        ctrls.forEach { childControlName ->
            controls[childControlName]?.parentControl = parentControlName
        }
    }

    @Composable
    override fun Display(renderContext: RenderContext, controlState: ControlState<Unit>, isRequired: Boolean) {
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
                        controlNames = ctrls,
                        columns = columns,
                        formSchema = this@SectionControl.formSchema,
                        formState = this@SectionControl.formState,
                        basePath = renderContext.basePath
                    )
                }
            }
        }
    }
}