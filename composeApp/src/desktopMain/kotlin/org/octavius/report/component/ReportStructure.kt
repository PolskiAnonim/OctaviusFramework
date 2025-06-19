package org.octavius.report.component

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
    val reportConfig: String // W przyszłości obiekt
) {

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