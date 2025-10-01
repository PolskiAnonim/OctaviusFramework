package org.octavius.data.builder

import org.octavius.data.ColumnInfo
import org.octavius.data.DataResult
import org.octavius.data.transaction.TransactionStep
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
    fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): DataResult<T?>

    // --- Zwracanie wartości skalarnych ---

    /** Pobiera pojedynczą wartość z pierwszej kolumny pierwszego wiersza. */
    fun <T : Any> toField(params: Map<String, Any?> = emptyMap()): DataResult<T?>

    /** Pobiera listę wartości z pierwszej kolumny wszystkich wierszy. */
    fun <T : Any> toColumn(params: Map<String, Any?> = emptyMap()): DataResult<List<T?>>

    // --- Pomocnicze ---

    /** Zwraca string SQL bez wykonywania zapytania. */
    fun toSql(): String
}

fun TerminalReturningMethods.toList(vararg params: Pair<String, Any?>): DataResult<List<Map<String, Any?>>> =
    toList(params.toMap())

fun TerminalReturningMethods.toSingle(vararg params: Pair<String, Any?>): DataResult<Map<String, Any?>?> =
    toSingle(params.toMap())

fun TerminalReturningMethods.toSingleWithColumnInfo(vararg params: Pair<String, Any?>): DataResult<Map<ColumnInfo, Any?>?> =
    toSingleWithColumnInfo(params.toMap())

fun <T : Any> TerminalReturningMethods.toListOf(
    kClass: KClass<T>,
    vararg params: Pair<String, Any?>
): DataResult<List<T>> =
    toListOf(kClass, params.toMap())

fun <T : Any> TerminalReturningMethods.toSingleOf(
    kClass: KClass<T>,
    vararg params: Pair<String, Any?>
): DataResult<T?> =
    toSingleOf(kClass, params.toMap())

fun <T : Any> TerminalReturningMethods.toField(vararg params: Pair<String, Any?>): DataResult<T?> =
    toField(params.toMap())

fun <T : Any> TerminalReturningMethods.toColumn(vararg params: Pair<String, Any?>): DataResult<List<T?>> =
    toColumn(params.toMap())


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
    fun execute(params: Map<String, Any?>): DataResult<Int>
}

fun TerminalModificationMethods.execute(vararg params: Pair<String, Any?>): DataResult<Int> =
    execute(params.toMap())

/**
 * Interfejs dla StepBuilder - zawiera te same metody terminalne co TerminalReturningMethods
 * i TerminalModificationMethods, ale zwraca ExtendedDatabaseStep zamiast wykonywać zapytania.
 */
interface StepBuilderMethods {
    // --- Zwracanie pełnych wierszy ---

    /** Tworzy ExtendedDatabaseStep z metodą toList */
    fun toList(params: Map<String, Any?> = emptyMap()): TransactionStep<List<Map<String, Any?>>>

    /** Tworzy ExtendedDatabaseStep z metodą toSingle */
    fun toSingle(params: Map<String, Any?> = emptyMap()): TransactionStep<Map<String, Any?>?>

    /** Tworzy ExtendedDatabaseStep z metodą toSingleWithColumnInfo */
    fun toSingleWithColumnInfo(params: Map<String, Any?> = emptyMap()): TransactionStep<Map<ColumnInfo, Any?>?>

    // --- Zwracanie obiektów data class ---

    /** Tworzy ExtendedDatabaseStep z metodą toListOf */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): TransactionStep<List<T>>

    /** Tworzy ExtendedDatabaseStep z metodą toSingleOf */
    fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): TransactionStep<T?>

    // --- Zwracanie wartości skalarnych ---

    /** Tworzy ExtendedDatabaseStep z metodą toField */
    fun <T : Any> toField(params: Map<String, Any?> = emptyMap()): TransactionStep<T?>

    /** Tworzy ExtendedDatabaseStep z metodą toColumn */
    fun <T : Any> toColumn(params: Map<String, Any?> = emptyMap()): TransactionStep<List<T?>>

    // --- Metoda modyfikująca ---

    /** Tworzy ExtendedDatabaseStep z metodą execute */
    fun execute(params: Map<String, Any?> = emptyMap()): TransactionStep<Int>
}

fun StepBuilderMethods.toList(vararg params: Pair<String, Any?>): TransactionStep<List<Map<String, Any?>>> =
    toList(params.toMap())

fun StepBuilderMethods.toSingle(vararg params: Pair<String, Any?>): TransactionStep<Map<String, Any?>?> =
    toSingle(params.toMap())

fun StepBuilderMethods.toSingleWithColumnInfo(vararg params: Pair<String, Any?>): TransactionStep<Map<ColumnInfo, Any?>?> =
    toSingleWithColumnInfo(params.toMap())

fun <T : Any> StepBuilderMethods.toField(vararg params: Pair<String, Any?>): TransactionStep<T?> =
    toField(params.toMap())

fun <T : Any> StepBuilderMethods.toColumn(vararg params: Pair<String, Any?>): TransactionStep<List<T?>> =
    toColumn(params.toMap())

fun StepBuilderMethods.execute(vararg params: Pair<String, Any?>): TransactionStep<Int> =
    execute(params.toMap())

inline fun <reified T : Any> StepBuilderMethods.toListOf(vararg params: Pair<String, Any?>): TransactionStep<List<T>> =
    toListOf(T::class, params.toMap())

/**
 * Wygodne funkcje rozszerzające inline dla StepBuilderMethods
 */
inline fun <reified T : Any> StepBuilderMethods.toListOf(params: Map<String, Any?> = emptyMap()): TransactionStep<List<T>> =
    toListOf(T::class, params)


inline fun <reified T : Any> StepBuilderMethods.toSingleOf(vararg params: Pair<String, Any?>): TransactionStep<T?> =
    toSingleOf(T::class, params.toMap())

inline fun <reified T : Any> StepBuilderMethods.toSingleOf(params: Map<String, Any?> = emptyMap()): TransactionStep<T?> =
    toSingleOf(T::class, params)

/** Interfejs oznaczający że builder może być konwertowany na StepBuilder */
interface StepConvertible {
    /** Konwertuje builder na StepBuilder dla wykonania jako kroku transackcji */
    fun asStep(): StepBuilderMethods
}