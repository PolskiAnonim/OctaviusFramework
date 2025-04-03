package org.octavius.novels.form.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable

abstract class Control<T: Any>(
    val label: String?,
    val fieldName: String?,
    val tableName: String?,
    val hidden: Boolean?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?
) {
    @Composable
    fun render(controlName: String, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val isVisible = shouldBeVisible(controls, states)

        AnimatedVisibility(visible = isVisible) {
            display(controlName, controls, states)
        }
    }

    @Composable
    protected abstract fun display(controlName: String, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>)

    @Composable
    private fun shouldBeVisible(controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Boolean {
        // Jeśli hidden jest true, zawsze ukryj
        if (hidden == true) return false

        // Sprawdź zależności
        if (dependencies != null) {
            for ((_, dependency) in dependencies) {
                val dependentControl = controls[dependency.controlName] ?: continue
                if (dependency.dependencyType != DependencyType.Hidden) continue

                val dependentState = states[dependency.controlName]
                val dependentValue = dependentState?.value?.value

                when (dependency.comparisonType) {
                    ComparisonType.OneOf -> {
                        // Sprawdź czy wartość kontrolki, od której zależy, jest na liście wartości
                        @Suppress("UNCHECKED_CAST")
                        val acceptedValues = dependency.value as? List<*> ?: listOf(dependency.value)
                        if (dependentValue !in acceptedValues) return false
                    }
                    ComparisonType.NotEquals -> {
                        // Sprawdź czy wartość kontrolki, od której zależy, jest różna od podanej
                        println(dependentValue)
                        println(dependency.value)
                        println(dependentValue == dependency.value)
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
    open fun setInitValue(value: Any?) : ControlState<T> {
        val state = ControlState<T>()
        if (value == null) {
            state.value.value = null
            return state
        }

        try {
            @Suppress("UNCHECKED_CAST")
            state.value.value = convertValue(value) as T
        } catch (e: ClassCastException) {
            println("Nie można skonwertować wartości $value na typ kontrolki ${this::class.simpleName}")
        }
        return state
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