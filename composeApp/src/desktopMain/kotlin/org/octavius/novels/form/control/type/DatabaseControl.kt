package org.octavius.novels.form.control.type

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.type.dropdown.DropdownControlBase
import org.octavius.novels.form.control.type.dropdown.DropdownOption

class DatabaseControl(
    columnInfo: ColumnInfo?,
    label: String?,
    private val relatedTable: String,
    private val displayColumn: String,
    private val pageSize: Int = 10,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : DropdownControlBase<Int>(
    label, columnInfo, required, dependencies
) {
    override val supportSearch = true
    override val supportPagination = true

    private var cachedValue: DropdownOption<Int>? = null

    override fun getDisplayText(value: Int?): String? {
        if (value == null) return value

        // Próbuj użyć cache
        if (cachedValue?.value == value) return cachedValue!!.displayText

        // Załaduj z bazy danych
        try {
            val sql = "SELECT $displayColumn FROM $relatedTable WHERE id = ?"
            val result = DatabaseManager.executeQuery(sql, listOf(value)).firstOrNull()

            if (result != null) {
                val displayValue = result[ColumnInfo(relatedTable, displayColumn)] as String
                cachedValue = DropdownOption(value, displayValue)
                return displayValue
            }
        } catch (e: Exception) {
            println("Błąd podczas pobierania tekstu wyświetlania: ${e.message}")
        }

        return value.toString()
    }

    override fun loadOptions(searchQuery: String, page: Int): Pair<List<DropdownOption<Int>>, Int> {
        val sql = if (searchQuery.isEmpty()) {
            "SELECT id, $displayColumn FROM $relatedTable ORDER BY $displayColumn"
        } else {
            "SELECT id, $displayColumn FROM $relatedTable WHERE $displayColumn ILIKE ? ORDER BY $displayColumn"
        }

        val params = if (searchQuery.isEmpty()) emptyList() else listOf("%$searchQuery%")

        return try {
            val (results, totalPages) = DatabaseManager.executePagedQuery(sql, params, page, pageSize)
            val mappedResults = results.groupBy { it[ColumnInfo(relatedTable, "id")] }
                .map { (id, items) ->
                    val displayValue = items.firstOrNull()?.get(ColumnInfo(relatedTable, displayColumn)) as? String ?: ""
                    DropdownOption(id as Int, displayValue)
                }
            Pair(mappedResults, totalPages.toInt())
        } catch (e: Exception) {
            println("Błąd podczas wyszukiwania elementów: ${e.message}")
            Pair(emptyList(), 1)
        }
    }
}