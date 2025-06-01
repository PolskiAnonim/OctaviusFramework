package org.octavius.novels.form.control.type

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
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

class SectionControl(
    val ctrls: List<String>,
    val collapsible: Boolean = true,
    val initiallyExpanded: Boolean = true,
    val columns: Int = 1,
    label: String,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Unit>(label, null, false, dependencies) {
    override val validator: ControlValidator<Unit> = DefaultValidator()

    override fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        ctrls.forEach { childControlName ->
            controls[childControlName]?.parentControl = parentControlName
        }
    }

    @Composable
    override fun Display(
        controlState: ControlState<Unit>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        val expanded = remember { mutableStateOf(initiallyExpanded) }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                            .padding(16.dp),
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
                                contentDescription = if (expanded.value) "Zwiń" else "Rozwiń",
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
                                .padding(16.dp)
                        ) {
                            if (columns > 1) {
                                val controlGroups = ctrls.chunked(ctrls.size / columns + if (ctrls.size % columns > 0) 1 else 0)

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    controlGroups.forEach { group ->
                                        Column(modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)) {
                                            group.forEach { ctrlName ->
                                                controls[ctrlName]?.let { control ->
                                                    control.Render(states[ctrlName]!!, controls, states)
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    ctrls.forEach { ctrlName ->
                                        controls[ctrlName]?.let { control ->
                                            control.Render(states[ctrlName]!!, controls, states)
                                            Spacer(modifier = Modifier.height(12.dp))
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