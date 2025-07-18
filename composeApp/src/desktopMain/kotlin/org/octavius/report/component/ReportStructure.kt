package org.octavius.report.component

import org.octavius.report.Query
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.special.ActionColumn

/**
 * ReportStructure
 * Klasa definiująca strukturę raportu - zapytanie SQL i kolumny
 * @property query zapytanie SQL do pobrania danych
 * @property columns definicje kolumn raportu jako mapa - nazwy nie mogą się powtarzać
 * @property columnOrder kolejność wyświetlania kolumn
 */
class ReportStructure(
    val query: Query,
    private val initColumns: Map<String, ReportColumn>,
    val reportName: String,
    val rowActions: List<ReportRowAction> = emptyList()
) {
    lateinit var columns: Map<String, ReportColumn>

    /**
     * Zwraca listę kluczy kolumn, które mogą być zarządzane przez użytkownika
     * w panelu konfiguracji (ukrywanie, zmiana kolejności).
     * Pomija kolumny specjalne (np. akcje).
     */
    val manageableColumnKeys: List<String> get() = initColumns.keys.toList()

    fun initSpecialColumns() {
        val specialColumns = mutableMapOf<String, ReportColumn>()
        if (rowActions.isNotEmpty()) {
            specialColumns.put("_actions", ActionColumn(rowActions))
        }
        columns = specialColumns + initColumns
    }

    /**
     * Zwraca kolumnę o danej nazwie
     */
    fun getColumn(name: String): ReportColumn {
        val column = columns[name]
        if (column == null) {
            NotImplementedError("Column with such name not found")
        }
        return columns[name]!!
    }

    /**
     * Zwraca wszystkie kolumny zdefiniowane w tej strukturze
     */
    fun getAllColumns(): Map<String, ReportColumn> = columns
}

/**
 * Fabryka do tworzenia struktur raportów
 * Służy do tworzenia klasy ReportStructure
 */
abstract class ReportStructureBuilder {
    fun build(): ReportStructure {
        val structure = buildStructure()
        structure.initSpecialColumns()
        return structure
    }

    abstract fun buildStructure(): ReportStructure
}