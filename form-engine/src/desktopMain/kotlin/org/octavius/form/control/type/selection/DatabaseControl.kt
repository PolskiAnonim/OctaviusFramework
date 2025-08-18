package org.octavius.form.control.type.selection

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.type.selection.dropdown.DropdownControlBase
import org.octavius.form.control.type.selection.dropdown.DropdownOption

/**
 * Kontrolka do wyboru rekordu z bazy danych z listy rozwijanej.
 *
 * Umożliwia wyszukiwanie i wybór rekordu z określonej tabeli bazy danych.
 * Obsługuje wyszukiwanie i paginację wyników. Wyświetla określoną kolumnę
 * jako tekst wyboru, a zwraca ID wybranego rekordu.
 */
class DatabaseControl(
    columnInfo: ColumnInfo?,
    label: String?,
    private val relatedTable: String,
    private val displayColumn: String,
    private val pageSize: Long = 10,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    actions: List<ControlAction<Int>>? = null,
) : DropdownControlBase<Int>(
    label, columnInfo, required, dependencies, actions
), KoinComponent {
    override val supportSearch = true
    override val supportPagination = true

    private val fetcher: DataFetcher by inject()

    private var cachedValue: DropdownOption<Int>? = null

    override fun getDisplayText(value: Int?): String? {
        if (value == null) return null

        // Próbuj użyć cache
        if (cachedValue?.value == value) return cachedValue!!.displayText

        // Załaduj z bazy danych
        try {
            val result = fetcher.select(displayColumn, from = relatedTable).where("id = :id")
                .toField<String>(mapOf("id" to value))

            if (result != null) {
                cachedValue = DropdownOption(value, result)
                return result
            }
        } catch (e: Exception) {
            println("Błąd podczas pobierania tekstu wyświetlania: ${e.message}")
        }

        return value.toString()
    }

    override fun loadOptions(searchQuery: String, page: Long): Pair<List<DropdownOption<Int>>, Long> {
        val filter = if (searchQuery.isEmpty()) {
            ""
        } else {
            "$displayColumn ILIKE :search"
        }

        val params = if (searchQuery.isEmpty()) emptyMap<String,Any>() else mapOf("search" to "%$searchQuery%")
        return try {
            val totalPages = fetcher.fetchCount(relatedTable, filter, params) / pageSize
            val results =
                fetcher.select("id, $displayColumn", from = relatedTable).where(filter.takeIf { it.isNotBlank() })
                    .orderBy(displayColumn)
                    .page(page, pageSize).toList(params = params)

            val mappedResults = results.map { DropdownOption(it["id"] as Int,it[displayColumn] as String) }
            Pair(mappedResults, totalPages)
        } catch (e: Exception) {
            println("Błąd podczas wyszukiwania elementów: ${e.message}")
            Pair(emptyList(), 1)
        }
    }
}