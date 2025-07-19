package org.octavius.form

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Opisuje relację między tabelami w zapytaniach JOIN.
 *
 * @param tableName nazwa tabeli
 * @param joinCondition warunek JOIN (pusty dla głównej tabeli)
 * @param primaryKey nazwa kolumny klucza głównego (domyślnie "id")
 */
data class TableRelation(
    val tableName: String,
    val joinCondition: String = "",
    val primaryKey: String = "id"
)

/**
 * Sealed class reprezentująca operacje zapisu w bazie danych.
 *
 * Wszystkie operacje zawierają nazwę tabeli i mogą używać foreign key
 * do definiowania relacji między rekordami.
 */
sealed class SaveOperation {
    abstract val tableName: String
    abstract val foreignKeys: List<ForeignKey>
    /**
     * Operacja wstawienia nowego rekordu.
     *
     * @param tableName nazwa tabeli
     * @param data dane do wstawienia (klucz -> wartość)
     * @param foreignKeys lista kluczy obcych
     * @param returningId czy zwrócić ID nowo utworzonego rekordu
     */
    data class Insert(
        override val tableName: String,
        val data: Map<String, Any?>,
        override val foreignKeys: List<ForeignKey> = emptyList(),
        val returningId: Boolean = true
    ) : SaveOperation()

    /**
     * Operacja aktualizacji istniejącego rekordu.
     *
     * @param tableName nazwa tabeli
     * @param data dane do zaktualizowania
     * @param id ID rekordu (może być null jeśli używamy foreign keys)
     * @param foreignKeys lista kluczy obcych do identyfikacji rekordu
     */
    data class Update(
        override val tableName: String,
        val data: Map<String, Any?>,
        val id: Int? = null,
        override val foreignKeys: List<ForeignKey> = emptyList()
    ) : SaveOperation()

    /**
     * Operacja usunięcia rekordu.
     *
     * @param tableName nazwa tabeli
     * @param id ID rekordu do usunięcia (może być null jeśli używamy foreign keys)
     * @param foreignKeys lista kluczy obcych do identyfikacji rekordu
     */
    data class Delete(
        override val tableName: String,
        val id: Int? = null,
        override val foreignKeys: List<ForeignKey> = emptyList()
    ) : SaveOperation()
}

/**
 * Reprezentuje klucz obcy w operacjach bazodanowych.
 *
 * @param columnName nazwa kolumny klucza obcego
 * @param referencedTable nazwa tabeli, do której odnosi się klucz
 * @param value wartość klucza (może być ustawiona później)
 */
data class ForeignKey(
    val columnName: String,
    val referencedTable: String,
    var value: Int? = null
)

/**
 * Dane wyniku kontrolki zebrane podczas przetwarzania formularza.
 *
 * @param currentValue wartość kontrolki przygotowana do zapisu w bazie danych
 * @param initialValue pierwotna wartość załadowana z bazy lub ustawiona domyślnie
 */
data class ControlResultData(
    val currentValue: Any?,
    val initialValue: Any?
)

/**
 * Stan pojedynczej kontrolki w formularzu.
 *
 * Przechowuje wszystkie informacje o stanie kontrolki potrzebne
 * do renderowania, walidacji i śledzenia zmian.
 *
 * @param T typ danych przechowywanych przez kontrolkę
 * @param value bieżąca wartość kontrolki (edytowana przez użytkownika)
 * @param initValue pierwotna wartość załadowana z bazy lub ustawiona domyślnie
 */
data class ControlState<T>(
    val value: MutableState<T?> = mutableStateOf(null),
    val initValue: MutableState<T?> = mutableStateOf(null)
)