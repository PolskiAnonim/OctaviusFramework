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
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.validator.DefaultValidator
import org.octavius.localization.Translations
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
) : Control<Unit>(label, null, false, dependencies, hasStandardLayout = false) {
    override val validator: ControlValidator<Unit> = DefaultValidator()

    override fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        ctrls.forEach { childControlName ->
            controls[childControlName]?.parentControl = parentControlName
        }
    }

    @Composable
    override fun Display(controlName: String, controlState: ControlState<Unit>, isRequired: Boolean) {
        val expanded = remember { mutableStateOf(initiallyExpanded) }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FormSpacing.containerPaddingVertical),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Nagłówek sekcji
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = collapsible) { expanded.value = !expanded.value },
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(FormSpacing.sectionPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        if (collapsible) {
                            Icon(
                                imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded.value) Translations.get("expandable.collapse") else Translations.get("expandable.expand"),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Zawartość sekcji
                AnimatedVisibility(visible = expanded.value) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FormSpacing.sectionPadding)
                        ) {
                            if (columns > 1) {
                                val controlGroups =
                                    ctrls.chunked(ctrls.size / columns + if (ctrls.size % columns > 0) 1 else 0)

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    controlGroups.forEach { group ->
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = FormSpacing.fieldPaddingHorizontal)
                                        ) {
                                            group.forEach { ctrlName ->
                                                val controls =
                                                    this@SectionControl.formSchema.getAllControls()
                                                val states = this@SectionControl.formState.getAllStates()
                                                controls[ctrlName]?.let { control ->
                                                    states[ctrlName]?.let { controlState ->
                                                        control.Render(ctrlName, controlState)
                                                    }
                                                    Spacer(modifier = Modifier.height(FormSpacing.sectionContentSpacing))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ctrls.forEach { ctrlName ->
                                        val controls = this@SectionControl.formSchema.getAllControls()
                                        val states = this@SectionControl.formState.getAllStates()
                                        controls[ctrlName]?.let { control ->
                                            states[ctrlName]?.let { controlState ->
                                                control.Render(ctrlName, controlState)
                                            }
                                            Spacer(modifier = Modifier.height(FormSpacing.sectionHeaderPaddingBottom))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}