package org.octavius.data.builder

import org.octavius.data.DataResult
import kotlin.reflect.KClass

/** Interfejs zawierający metody terminalne zwracające dane */
interface TerminalReturningMethods {
    // --- Zwracanie pełnych wierszy ---

    /** Pobiera listę wierszy jako List<Map<String, Any?>>. */
    fun toList(params: Map<String, Any?> = emptyMap()): DataResult<List<Map<String, Any?>>>

    /** Pobiera pojedynczy wiersz jako Map<String, Any?>?. */
    fun toSingle(params: Map<String, Any?> = emptyMap()): DataResult<Map<String, Any?>?>

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
    fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<T?>

    // --- Zwracanie wartości skalarnych ---

    /** Pobiera pojedynczą wartość z pierwszej kolumny pierwszego wiersza. */
    fun <T : Any> toField(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<T?>

    /** Pobiera listę wartości z pierwszej kolumny wszystkich wierszy. */
    fun <T : Any> toColumn(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<List<T?>>

    // --- Pomocnicze ---

    /** Zwraca string SQL bez wykonywania zapytania. */
    fun toSql(): String
}

fun TerminalReturningMethods.toList(vararg params: Pair<String, Any?>): DataResult<List<Map<String, Any?>>> =
    toList(params.toMap())

fun TerminalReturningMethods.toSingle(vararg params: Pair<String, Any?>): DataResult<Map<String, Any?>?> =
    toSingle(params.toMap())

inline fun <reified T : Any> TerminalReturningMethods.toField(
    params: Map<String, Any?> = emptyMap()
): DataResult<T?> = toField(T::class, params)

inline fun <reified T : Any> TerminalReturningMethods.toField(
    vararg params: Pair<String, Any?>
): DataResult<T?> = toField(T::class, params.toMap())

inline fun <reified T : Any> TerminalReturningMethods.toColumn(
    params: Map<String, Any?> = emptyMap()
): DataResult<List<T?>> = toColumn(T::class, params)

inline fun <reified T : Any> TerminalReturningMethods.toColumn(
    vararg params: Pair<String, Any?>
): DataResult<List<T?>> = toColumn(T::class, params.toMap())

inline fun <reified T : Any> TerminalReturningMethods.toListOf(vararg params: Pair<String, Any?>): DataResult<List<T>> =
    toListOf(T::class, params.toMap())

/**
 * Wygodna funkcja rozszerzająca `inline` dla toListOf.
 * Wykorzystuje `reified` do automatycznego wnioskowania `T::class`.
 */
inline fun <reified T : Any> TerminalReturningMethods.toListOf(params: Map<String, Any?> = emptyMap()): DataResult<List<T>> =
    toListOf(T::class, params)

inline fun <reified T : Any> TerminalReturningMethods.toSingleOf(vararg params: Pair<String, Any?>): DataResult<T?> =
    toSingleOf(T::class, params.toMap())

/**
 * Wygodna funkcja rozszerzająca `inline` dla toSingleOf.
 * Wykorzystuje `reified` do automatycznego wnioskowania `T::class`.
 */
inline fun <reified T : Any> TerminalReturningMethods.toSingleOf(params: Map<String, Any?> = emptyMap()): DataResult<T?> =
    toSingleOf(T::class, params)


/** Interfejs zawierający metodę terminalną modyfikującą*/
interface TerminalModificationMethods {
    /** Wykonuje zapytanie i zwraca liczbę wierszy która została zaktualizowana */
    fun execute(params: Map<String, Any?> = emptyMap()): DataResult<Int>
}

fun TerminalModificationMethods.execute(vararg params: Pair<String, Any?>): DataResult<Int> =
    execute(params.toMap())
