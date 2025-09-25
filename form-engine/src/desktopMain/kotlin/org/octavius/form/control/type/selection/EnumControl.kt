package org.octavius.form.control.type.selection

import org.octavius.data.ColumnInfo
import org.octavius.domain.EnumWithFormatter
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.type.selection.dropdown.DropdownControlBase
import org.octavius.form.control.type.selection.dropdown.DropdownOption
import kotlin.reflect.KClass

/**
 * Kontrolka do wyboru wartości z enumeracji (enum) z listy rozwijanej.
 *
 * Automatycznie generuje opcje wyboru na podstawie wartości enumeracji.
 * Wymaga aby enum implementował interfejs EnumWithFormatter dla formatowania
 * tekstu wyświetlanego użytkownikowi. Obsługuje wyszukiwanie w opcjach.
 */
class EnumControl<T>(
    columnInfo: ColumnInfo?,
    label: String?,
    private val enumClass: KClass<T>,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    actions: List<ControlAction<T>>? = null
) : DropdownControlBase<T>(
    label, columnInfo, required, dependencies, actions
) where T : Enum<T>, T : EnumWithFormatter<T> {

    override fun getDisplayText(value: T?): String? {
        return value?.toDisplayString()
    }

    override fun loadOptions(searchQuery: String, page: Long): Pair<List<DropdownOption<T>>, Long> {
        val options = enumClass.java.enumConstants
            .filter {
                searchQuery.isEmpty() ||
                        it.toDisplayString().contains(searchQuery, ignoreCase = true)
            }
            .map { DropdownOption(it, it.toDisplayString()) }

        return Pair(options, 1L) // Enumy zawsze mają jedną stronę
    }
}