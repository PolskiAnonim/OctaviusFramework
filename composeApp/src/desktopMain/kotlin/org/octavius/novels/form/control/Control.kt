package org.octavius.novels.form.control

import androidx.compose.runtime.Composable

abstract class Control <T: Any>(
    val state: ControlState<T>?,
    val label: String?,
    val fieldName: String?,
    val tableName: String?,
    val hidden: String?,
    val required: String?,
    val dependencies: Map<String, ControlDependency<*>>?
) {
    @Composable
    abstract fun display(controls: Map<String, Control<*>>)
}

data class ControlDependency<T>(
    val controlName: String,
    val value: T,
    val dependencyType: DependencyType
)

enum class DependencyType {
    Equals,
    NotEquals,
}
