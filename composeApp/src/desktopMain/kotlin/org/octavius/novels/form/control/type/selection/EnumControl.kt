package org.octavius.novels.form.control.type.selection

import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.domain.EnumWithFormatter
import org.octavius.novels.form.control.base.ControlDependency
import org.octavius.novels.form.control.type.selection.dropdown.DropdownControlBase
import org.octavius.novels.form.control.type.selection.dropdown.DropdownOption
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
    dependencies: Map<String, ControlDependency<*>>? = null
) : DropdownControlBase<T>(
    label, columnInfo, required, dependencies
) where T : Enum<T>, T : EnumWithFormatter<T> {

    override fun getDisplayText(value: T?): String? {
        return value?.toDisplayString()
    }

    override fun loadOptions(searchQuery: String, page: Int): Pair<List<DropdownOption<T>>, Int> {
        val options = enumClass.java.enumConstants
            .filter {
                searchQuery.isEmpty() ||
                        it.toDisplayString().contains(searchQuery, ignoreCase = true)
            }
            .map { DropdownOption(it, it.toDisplayString()) }

        return Pair(options, 1) // Enumy zawsze mają jedną stronę
    }
}