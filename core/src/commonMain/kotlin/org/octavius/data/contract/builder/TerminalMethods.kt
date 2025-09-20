package org.octavius.data.contract.builder

import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataResult
import kotlin.reflect.KClass

/** Interfejs zawierający metody terminalne zwracające dane */
interface TerminalReturningMethods {
    // --- Zwracanie pełnych wierszy ---

    /** Pobiera listę wierszy jako List<Map<String, Any?>>. */
    fun toList(params: Map<String, Any?> = emptyMap()): DataResult<List<Map<String, Any?>>>

    /** Pobiera pojedynczy wiersz jako Map<String, Any?>?. */
    fun toSingle(params: Map<String, Any?> = emptyMap()): DataResult<Map<String, Any?>?>

    /** Pobiera pojedynczy wiersz z pełnymi informacjami o kolumnach (tabela + nazwa). */
    fun toSingleWithColumnInfo(params: Map<String, Any?> = emptyMap()): DataResult<Map<ColumnInfo, Any?>?>

    // --- Zwracanie obiektów data class ---

    /**
     * Mapuje wyniki na listę obiektów danego typu.
     * Wymaga, aby nazwy/aliasy kolumn w SQL (w konwencji snake_case) odpowiadały
     * nazwom właściwości w klasie `kClass` (w konwencji camelCase) lub posiadały adnotację @MapKey
     * z zapisaną nazwą kolumny.
     */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<List<T>>

    /**
     * Mapuje wynik na pojedynczy obiekt danego typu.
     * Działa na tej samej zasadzie mapowania co `toListOf`.
     */
    fun <T: Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<T?>

    // --- Zwracanie wartości skalarnych ---

    /** Pobiera pojedynczą wartość z pierwszej kolumny pierwszego wiersza. */
    fun <T> toField(params: Map<String, Any?> = emptyMap()): DataResult<T?>

    /** Pobiera listę wartości z pierwszej kolumny wszystkich wierszy. */
    fun <T> toColumn(params: Map<String, Any?> = emptyMap()): DataResult<List<T?>>

    // --- Pomocnicze ---

    /** Zwraca string SQL bez wykonywania zapytania. */
    fun toSql(): String
}

/**
 * Wygodna funkcja rozszerzająca `inline` dla toListOf.
 * Wykorzystuje `reified` do automatycznego wnioskowania `T::class`.
 */
inline fun <reified T : Any> TerminalReturningMethods.toListOf(params: Map<String, Any?> = emptyMap()): DataResult<List<T>> {
    return this.toListOf(T::class, params)
}

/**
 * Wygodna funkcja rozszerzająca `inline` dla toSingleOf.
 * Wykorzystuje `reified` do automatycznego wnioskowania `T::class`.
 */
inline fun <reified T : Any> TerminalReturningMethods.toSingleOf(params: Map<String, Any?> = emptyMap()): DataResult<T?> {
    return this.toSingleOf(T::class, params)
}

/** Interfejs zawierający metodę terminalną modyfikującą*/
interface TerminalModificationMethods {
    /** Wykonuje zapytanie i zwraca liczbę wierszy która została zaktualizowana */
    fun execute(params: Map<String, Any?>): DataResult<Int>
}