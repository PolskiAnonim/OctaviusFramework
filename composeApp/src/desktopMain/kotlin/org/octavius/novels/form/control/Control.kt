package org.octavius.novels.form.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.validation.ControlValidator

abstract class Control<T: Any>(
    val label: String?,
    val fieldName: String?,
    val tableName: String?,
    val hidden: Boolean?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?
) {
    // Tworzymy validator jako lazy property
    protected abstract val validator: ControlValidator<T>

    open fun updateState(state: ControlState<T>) {
        if (state.value.value != state.initValue.value) {
            state.dirty.value = true
        }
    }

    // Walidacja
    // Uproszczona metoda walidacji
    open fun validateControl(state: ControlState<*>?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        if (state == null) return
        validator.validate(state, this, controls, states)
    }


    // Konwersja wyniku
    open fun getResult(value: Any?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Any? {
        if (!validator.isControlVisible(this, controls, states)) return null
        return convertToResult(value)
    }

    protected open fun convertToResult(value: Any?): Any? {
        return value
    }

    @Composable
    fun render(controlState: ControlState<*>?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val isVisible = validator.isControlVisible(this, controls, states)

        AnimatedVisibility(visible = isVisible) {
            @Suppress("UNCHECKED_CAST")
            display(controlState as ControlState<T>?, controls, states)
        }
    }

    @Composable
    protected abstract fun display(controlState: ControlState<T>?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>)

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