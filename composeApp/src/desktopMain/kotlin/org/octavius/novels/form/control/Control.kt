package org.octavius.novels.form.control

import androidx.compose.runtime.Composable

abstract class Control <T: Any>(
    val state: ControlState<T>?,
    val label: String?,
    val fieldName: String?,
    val tableName: String?,
    val hidden: Boolean?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?
) {
    @Composable
    abstract fun display(controls: Map<String, Control<*>>)

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
    val dependencyType: DependencyType
)

enum class DependencyType {
    OneOf,
    NotEquals,
}
