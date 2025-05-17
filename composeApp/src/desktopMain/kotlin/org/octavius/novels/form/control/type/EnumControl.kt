package org.octavius.novels.form.control.type

import org.octavius.novels.domain.EnumWithFormatter
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.type.dropdown.DropdownControlBase
import org.octavius.novels.form.control.type.dropdown.DropdownOption
import kotlin.reflect.KClass

class EnumControl<T>(
    columnInfo: ColumnInfo?,
    label: String?,
    private val enumClass: KClass<T>,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : DropdownControlBase<T>(
    label, columnInfo, hidden, required, dependencies
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