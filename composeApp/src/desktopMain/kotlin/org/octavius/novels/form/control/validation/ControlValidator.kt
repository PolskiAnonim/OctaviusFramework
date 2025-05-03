package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.DependencyType

abstract class ControlValidator<T: Any> {
    // Sprawdzenie czy kontrolka jest widoczna
    fun isControlVisible(
        control: Control<*>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ): Boolean {
        // Jeśli kontrolka ma rodzica, najpierw sprawdź czy rodzic jest widoczny
        control.parentControl?.let { parentName ->
            val parentControl = controls[parentName] ?: return false
            if (!isControlVisible(parentControl, controls, states)) return false
        }
        // Jeśli hidden jest true, zawsze ukryj
        if (control.hidden == true) return false

        // Sprawdź zależności
        control.dependencies?.forEach { (_, dependency) ->
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

    // Sprawdzenie czy pole jest wymagane
    fun isControlRequired(
        control: Control<*>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ): Boolean {
        var isRequired = control.required == true

        // Sprawdzamy zależności typu Required
        control.dependencies?.forEach { (_, dependency) ->
            if (dependency.dependencyType == DependencyType.Required) {
                val dependentControl = controls[dependency.controlName] ?: return@forEach
                val dependentState = states[dependency.controlName] ?: return@forEach
                val dependentValue = dependentState.value.value

                when (dependency.comparisonType) {
                    ComparisonType.OneOf -> {
                        @Suppress("UNCHECKED_CAST")
                        val acceptedValues = dependency.value as? List<*> ?: listOf(dependency.value)
                        if (dependentValue in acceptedValues) {
                            isRequired = true
                        }
                    }
                    ComparisonType.Equals -> {
                        if (dependentValue == dependency.value) {
                            isRequired = true
                        }
                    }
                    ComparisonType.NotEquals -> {
                        if (dependentValue != dependency.value) {
                            isRequired = true
                        }
                    }
                }
            }
        }

        return isRequired
    }

    // Sprawdzenie, czy wartość jest pusta
    fun isValueEmpty(value: Any?): Boolean {
        return when (value) {
            null -> true
            is String -> value.isBlank()
            is List<*> -> value.isEmpty()
            else -> false
        }
    }

    // Główna metoda walidacji
    fun validate(
        controlName: String,
        state: ControlState<*>,
        control: Control<*>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ) {
        // Jeśli kontrolka nie jest widoczna, pomijamy walidację
        if (!isControlVisible(control, controls, states)) {
            return
        }

        // Sprawdzamy, czy pole jest wymagane
        val isRequired = isControlRequired(control, controls, states)

        // Jeśli pole jest wymagane i wartość jest pusta, ustawiamy błąd
        if (isRequired && isValueEmpty(state.value.value)) {
            state.error.value = "To pole jest wymagane"
            return
        }

        // Wywołujemy dodatkową walidację specyficzną dla kontrolki
        validateSpecific(state)
    }

    // Metoda do nadpisania przez klasy pochodne
    protected open fun validateSpecific(state: ControlState<*>) {
        // Domyślnie nic nie robimy
    }
}