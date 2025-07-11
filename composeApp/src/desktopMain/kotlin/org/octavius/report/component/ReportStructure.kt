package org.octavius.report.component

import org.octavius.report.Query
import org.octavius.report.ReportAction
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
    val reportConfig: String, // W przyszłości obiekt
    val reportName: String,
    val rowActions: List<ReportAction> = emptyList()
) {
    lateinit var columns: Map<String, ReportColumn>

    fun initSpecialColumns(reportState: ReportState) {
        val specialColumns = mutableMapOf<String, ReportColumn>()
        if (rowActions.isNotEmpty()) {
            specialColumns.put("_actions", ActionColumn(rowActions, reportState))
        }
        columns = specialColumns + initColumns
    }

    /**
     * Zwraca kolumnę o danej nazwie
     */
    fun getColumn(name: String): ReportColumn? = columns[name]

    /**
     * Zwraca wszystkie kolumny zdefiniowane w tej strukturze
     */
    fun getAllColumns(): Map<String, ReportColumn> = columns
}

/**
 * Fabryka do tworzenia struktur raportów
 * Zawiera jedną funkcję - build
 * Służy do tworzenia klasy ReportStructure
 */
abstract class ReportStructureBuilder {
    abstract fun build(): ReportStructure
}