package org.octavius.novels.form.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.validation.ControlValidator

abstract class Control<T: Any>(
    val label: String?,
    val columnInfo: ColumnInfo?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?,
    var parentControl: String? = null // Kontrolka nadrzędna
) {
    // Tworzymy validator jako lazy property
    protected abstract val validator: ControlValidator<T>

    // Funkcja do ustawienia zależności parentCtrl - wykorzystywana gdy kontrolka powinna zależeć od widoczności
    // kontrolki nadrzędnej
    open fun setupParentRelationships(parentControlName: String ,controls: Map<String, Control<*>>) {
        return // Domyślnie brak działania
    }

    fun updateState(state: ControlState<T>) {
        if (state.value.value != state.initValue.value) {
            state.dirty.value = true
        }
    }

    // Walidacja
    // Uproszczona metoda walidacji
    fun validateControl(controlName: String, state: ControlState<*>?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        if (state == null) return
        validator.validate(controlName, state, this, controls, states)
    }


    // Konwersja wyniku
    fun getResult(
        state: ControlState<*>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ): Any? {
        if (!validator.isControlVisible(this, controls, states)) return null
        return convertToResult(state, controls, states)
    }

    protected open fun convertToResult(state: ControlState<*>, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Any? {
        return state.value.value
    }

    // Metoda do ustawiania wartości
    open fun setInitValue(value: Any?) : ControlState<T> {
        val state = ControlState<T>()
        if (value == null) {
            return state
        }

        try {
            @Suppress("UNCHECKED_CAST")
            state.initValue.value = value as T
            state.value.value = copyInitToValue(value)
        } catch (e: ClassCastException) {
            println("Nie można skonwertować wartości $value na typ kontrolki ${this::class.simpleName}")
        }
        return state
    }

    // Funkcja jest wymagana w przypadku kontrolek potrzebujących głębokich kopii
    open fun copyInitToValue(value: T): T {
        // Dla podstawowych typów kopia będzie automatyczna
        @Suppress("UNCHECKED_CAST")
        return value
    }

    //---------------------------------------------Wyświetlanie kontrolek-----------------------------------------------
    // Funkcja służąca do ogólnego wyświetlania kontrolki
    @Composable
    fun Render(controlState: ControlState<*>, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val isVisible = validator.isControlVisible(this, controls, states)
        val isRequired = validator.isControlRequired(this, controls, states)
        AnimatedVisibility(visible = isVisible) {
            @Suppress("UNCHECKED_CAST")
            Display(controlState as ControlState<T>, controls, states, isRequired)
        }
    }

    @Composable
    protected abstract fun Display(controlState: ControlState<T>, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>, isRequired: Boolean)
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