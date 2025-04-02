package org.octavius.novels.form.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable

abstract class Control<T: Any>(
    val state: ControlState<T>?,
    val label: String?,
    val fieldName: String?,
    val tableName: String?,
    val hidden: Boolean?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?
) {
    @Composable
    fun render(controls: Map<String, Control<*>>) {
        val isVisible = shouldBeVisible(controls)

        AnimatedVisibility(visible = isVisible) {
            display(controls)
        }
    }

    @Composable
    protected abstract fun display(controls: Map<String, Control<*>>)

    @Composable
    private fun shouldBeVisible(controls: Map<String, Control<*>>): Boolean {
        // Jeśli hidden jest true, zawsze ukryj
        if (hidden == true) return false

        // Sprawdź zależności
        if (dependencies != null) {
            for ((_, dependency) in dependencies) {
                val dependentControl = controls[dependency.controlName] ?: continue
                if (dependency.dependencyType != DependencyType.Hidden) continue

                val dependentValue = dependentControl.state?.value?.value

                when (dependency.comparisonType) {
                    ComparisonType.OneOf -> {
                        // Sprawdź czy wartość kontrolki, od której zależy, jest na liście wartości
                        @Suppress("UNCHECKED_CAST")
                        val acceptedValues = dependency.value as? List<*> ?: listOf(dependency.value)
                        if (dependentValue !in acceptedValues) return false
                    }
                    ComparisonType.NotEquals -> {
                        // Sprawdź czy wartość kontrolki, od której zależy, jest różna od podanej
                        if (dependentValue == dependency.value) return false
                    }
                    ComparisonType.Equals -> {
                        if (dependentValue != dependency.value) return false
                    }
                }
            }
        }

        return true
    }

    // Metoda do ustawiania wartości
    open fun setInitValue(value: Any?) {
        if (value == null) {
            state?.value?.value = null
            return
        }

        try {
            @Suppress("UNCHECKED_CAST")
            state?.value?.value = convertValue(value) as T
        } catch (e: ClassCastException) {
            println("Nie można skonwertować wartości $value na typ kontrolki ${this::class.simpleName}")
        }
    }

    // Metoda pomocnicza do konwersji wartości - domyślna implementacja
    protected open fun convertValue(value: Any): Any? {
        return value
    }
}

data class ControlDependency<T>(
    val controlName: String,
    val value: T,
    val dependencyType: DependencyType,
    val comparisonType: ComparisonType
    )

enum class DependencyType {
    Hidden,
    Required,
}

enum class ComparisonType {
    OneOf,
    Equals,
    NotEquals
}