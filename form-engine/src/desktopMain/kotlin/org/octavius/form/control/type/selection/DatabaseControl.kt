package org.octavius.form.control.type.selection

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
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

        val result = fetcher.select(displayColumn, from = relatedTable).where("id = :id")
            .toField<String>(mapOf("id" to value))

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                null
            }
            is DataResult.Success<String?> -> {
                when (result.value) {
                    null -> null
                    else -> {
                        cachedValue = DropdownOption(value, result.value!!)
                        result.value
                    }
                }
            }
        }
    }

    override fun loadOptions(searchQuery: String, page: Long): Pair<List<DropdownOption<Int>>, Long> {
        // Krok 1: Przygotuj filtr i parametry
        val filter = if (searchQuery.isNotBlank()) "$displayColumn ILIKE :search" else null
        val params = if (searchQuery.isNotBlank()) mapOf("search" to "%$searchQuery%") else emptyMap()

        // Krok 2: Pobierz całkowitą liczbę pasujących rekordów
        val countResult = fetcher.query().from(relatedTable).where(filter).toCount(params)

        val totalCount = when (countResult) {
            is DataResult.Success -> countResult.value
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(countResult.error))
                return Pair(emptyList(), 0L)
            }
        }

        if (totalCount == 0L) {
            return Pair(emptyList(), 0L)
        }

        val totalPages = (totalCount + pageSize - 1) / pageSize


        val optionsResult = fetcher.select("id, $displayColumn", from = relatedTable)
            .where(filter)
            .orderBy(displayColumn)
            .page(page, pageSize)
            .toList(params = params)

        return when (optionsResult) {
            is DataResult.Success -> {
                val mappedOptions = optionsResult.value.map { row ->
                    val id = row["id"] as Int
                    val text = row[displayColumn] as String
                    DropdownOption(id, text)
                }
                Pair(mappedOptions, totalPages)
            }
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(optionsResult.error))
                Pair(emptyList(), 0L)
            }
        }
    }
}