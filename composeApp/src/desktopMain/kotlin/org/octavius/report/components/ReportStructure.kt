package org.octavius.report.components

import org.octavius.report.Query
import org.octavius.report.column.ReportColumn

/**
 * ReportStructure
 * Klasa definiująca strukturę raportu - zapytanie SQL i kolumny
 * @property query zapytanie SQL do pobrania danych
 * @property columns definicje kolumn raportu jako mapa - nazwy nie mogą się powtarzać
 * @property columnOrder kolejność wyświetlania kolumn
 */
class ReportStructure(
    val query: Query,
    private val columns: Map<String, ReportColumn>,
    val columnOrder: List<String>
) {
    
    init {
        // Sprawdź czy wszystkie kolumny z columnOrder istnieją w mapie columns
        columnOrder.forEach { columnName ->
            if (!columns.containsKey(columnName)) {
                throw IllegalArgumentException("Column '$columnName' not found in columns map")
            }
        }
    }

    /**
     * Zwraca kolumnę o danej nazwie
     */
    fun getColumn(name: String): ReportColumn? = columns[name]

    /**
     * Zwraca wszystkie kolumny zdefiniowane w tej strukturze
     */
    fun getAllColumns(): Map<String, ReportColumn> = columns
    
    /**
     * Zwraca kolumny w kolejności wyświetlania
     */
    fun getOrderedColumns(): Map<String, ReportColumn> {
        return columnOrder.associateWith { columns[it]!! }
    }
}

/**
 * Fabryka do tworzenia struktur raportów
 * Zawiera jedną funkcję - build
 * Służy do tworzenia klasy ReportStructure
 */
abstract class ReportStructureBuilder {
    abstract fun build(): ReportStructure
}