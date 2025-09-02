package org.octavius.report.component

import org.octavius.report.Query
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.special.ActionColumn
import org.octavius.report.column.type.special.QuickActionColumn

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
    val rowActions: List<ReportRowAction> = emptyList(),
    val defaultRowAction: ReportRowAction? = null,
    val addActions: List<ReportMainAction> = emptyList()
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
        if (defaultRowAction != null) {
            specialColumns.put("_quick_action", QuickActionColumn(defaultRowAction))
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
 * Fabryka do tworzenia struktur raportów.
 * Używa wzorca "Template Method" do zdefiniowania szkieletu budowania raportu.
 * Klasy dziedziczące muszą dostarczyć implementacje dla poszczególnych części raportu.
 */
abstract class ReportStructureBuilder {
    /**
     * Główna metoda budująca. Orkiestruje proces i nie powinna być nadpisywana.
     */
    fun build(): ReportStructure {
        val structure = buildStructure()
        structure.initSpecialColumns()
        return structure
    }

    /**
     * Metoda szablonowa, która składa raport z części dostarczonych
     * przez metody abstrakcyjne.
     */
    private fun buildStructure(): ReportStructure {
        return ReportStructure(
            query = buildQuery(),
            initColumns = buildColumns(),
            reportName = getReportName(),
            rowActions = buildRowActions(),
            defaultRowAction = buildDefaultRowAction(),
            addActions = buildMainActions()
        )
    }

    /**
     * Zwraca nazwę raportu.
     */
    abstract fun getReportName(): String

    /**
     * Buduje i zwraca obiekt Query dla raportu.
     */
    abstract fun buildQuery(): Query

    /**
     * Buduje i zwraca mapę kolumn dla raportu.
     */
    abstract fun buildColumns(): Map<String, ReportColumn>

    /**
     * Buduje i zwraca listę akcji dla wierszy.
     * Domyślnie zwraca pustą listę, ponieważ nie każdy raport ma akcje.
     */
    open fun buildRowActions(): List<ReportRowAction> = emptyList()

    /**
     * Buduje i zwraca domyślną akcję dla wierszy.
     * Domyślnie zwraca null.
     */
    open fun buildDefaultRowAction(): ReportRowAction? = null

    /**
     * Buduje i zwraca listę akcji dla menu "Dodaj".
     * Domyślnie zwraca pustą listę.
     */
    open fun buildMainActions(): List<ReportMainAction> = emptyList()
}