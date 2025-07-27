package org.octavius.data.contract

/**
 * Interfejs definiujący kontrakt dla operacji pobierania danych (SELECT).
 *
 * Abstrakcja nad mechanizmem odczytu z bazy danych, umożliwiająca
 * uniezależnienie logiki biznesowej od konkretnej implementacji (np. JDBC, JPA).
 */
interface DataFetcher {
    fun fetchCount(table: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Long
    fun fetchField(table: String, field: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Any?
    fun fetchRow(table: String, columns: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Map<String, Any?>
    fun fetchRowOrNull(table: String, columns: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Map<String, Any?>?
    fun fetchColumn(table: String, column: String, filter: String? = null, orderBy: String? = null, params: Map<String, Any?> = emptyMap()): List<Any?>
    fun fetchPagedColumn(table: String, column: String, offset: Int, limit: Int, filter: String? = null, orderBy: String? = null, params: Map<String, Any?> = emptyMap()): List<Any?>
    fun fetchList(table: String, columns: String, filter: String? = null, orderBy: String? = null, params: Map<String, Any?> = emptyMap()): List<Map<String, Any?>>
    fun fetchPagedList(table: String, columns: String, offset: Int, limit: Int, filter: String? = null, orderBy: String? = null, params: Map<String, Any?> = emptyMap()): List<Map<String, Any?>>
    fun fetchEntity(tables: String, filter: String? = null, params: Map<String, Any?> = emptyMap()): Map<ColumnInfo, Any?>
}