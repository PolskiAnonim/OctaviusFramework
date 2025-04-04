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

    // Walidacja
    open fun validateControl(state: ControlState<*>? ,controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        if (state == null) return
        if (!isControlVisible(controls, states)) return
        validate(state)
    }

    // Funkcja dla walidacji określonych kontrolek
    protected open fun validate(state: ControlState<*>) {
        return
    }

    // Konwersja wyniku

    open fun getResult(value: Any?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Any? {
        if (!isControlVisible(controls, states)) return null
        return convertToResult(value)
    }

    protected open fun convertToResult(value: Any?): Any? {
        return value
    }

    // Sprawdzenie czy kontrolka jest widoczna
    private fun isControlVisible(controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Boolean {
        // Jeśli hidden jest true, zawsze ukryj
        if (hidden == true) return false

        // Sprawdź zależności
        dependencies?.forEach { (_, dependency) ->
            if (dependency.dependencyType != DependencyType.Visible) return@forEach

            val dependentControl = controls[dependency.controlName] ?: return@forEach
            val dependentState = states[dependency.controlName] ?: return@forEach
            val dependentValue = dependentState.value.value

            when (dependency.comparisonType) {
                ComparisonType.OneOf -> {
                    @Suppress("UNCHECKED_CAST")
                    val acceptedValues = dependency.value as? List<*> ?: listOf(dependency.value)
                    if (dependentValue !in acceptedValues) return false
                }
                ComparisonType.NotEquals -> {
                    if (dependentValue == dependency.value) return false
                }
                ComparisonType.Equals -> {
                    if (dependentValue != dependency.value) return false
                }
            }
        }

        return true
    }

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
                if (dependency.dependencyType != DependencyType.Visible) continue

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
    Visible,
    Required,
}

enum class ComparisonType {
    OneOf,
    Equals,
    NotEquals
}