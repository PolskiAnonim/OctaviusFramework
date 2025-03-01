package org.octavius.novels.form.control.type

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency

class SectionControl(
    val ctrls: List<String>,
    val collapsible: Boolean = true,
    val initiallyExpanded: Boolean = true,
    val columns: Int = 1,
    label: String,
    hidden: String? = null,
    required: String? = null,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<Unit>(null,label, null, null, hidden, required, dependencies) {

    @Composable
    override fun display(controls: Map<String, Control<*>>) {
        // Stan zwinięcia/rozwinięcia sekcji
        val expanded = remember { mutableStateOf(initiallyExpanded) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Nagłówek sekcji z możliwością zwijania
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface)
                    .padding(8.dp)
                    .clickable(enabled = collapsible) { expanded.value = !expanded.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label ?: "",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface
                )

                if (collapsible) {
                    Icon(
                        imageVector = if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded.value) "Zwiń" else "Rozwiń",
                        tint = MaterialTheme.colors.onSurface
                    )
                }
            }

            // Zawartość sekcji (jeśli rozwinięta)
            AnimatedVisibility(visible = expanded.value) {
                // Logika układu w kolumnach
                if (columns > 1) {
                    // Podziel kontrolki na grupy według liczby kolumn
                    val controlGroups = ctrls.chunked(ctrls.size / columns + if (ctrls.size % columns > 0) 1 else 0)

                    Row(modifier = Modifier.fillMaxWidth()) {
                        controlGroups.forEach { group ->
                            Column(modifier = Modifier.weight(1f)) {
                                group.forEach { ctrlName ->
                                    controls[ctrlName]?.let { control ->
                                        control.display(controls)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Standardowy układ jednokolumnowy
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ctrls.forEach { ctrlName ->
                            controls[ctrlName]?.let { control ->
                                control.display(controls)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}