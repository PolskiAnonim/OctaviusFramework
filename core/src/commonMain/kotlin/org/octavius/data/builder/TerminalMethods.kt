package org.octavius.data.builder

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

    /**
     * Wykonuje zapytanie i przetwarza każdy wiersz indywidualnie za pomocą podanej akcji,
     * nie ładując całego wyniku do pamięci. Idealne dla dużych zbiorów danych.
     * Zwraca DataResult.Success(Unit) jeśli operacja się powiodła lub DataResult.Failure w razie błędu.
     *
     * @param params Parametry zapytania.
     * @param action Funkcja, która zostanie wykonana dla każdego wiersza. Wiersz jest mapowany na Map<String, Any?>.
     */
    fun forEachRow(params: Map<String, Any?> = emptyMap(), action: (row: Map<String, Any?>) -> Unit): DataResult<Unit>

    /**
     * Wykonuje zapytanie i przetwarza każdy wiersz indywidualnie za pomocą podanej akcji,
     * nie ładując całego wyniku do pamięci. Idealne dla dużych zbiorów danych.
     * Zwraca DataResult.Success(Unit) jeśli operacja się powiodła lub DataResult.Failure w razie błędu.
     *
     * @param params Parametry zapytania.
     * @param action Funkcja, która zostanie wykonana dla każdego wiersza. Wiersz jest mapowany na data class.
     */
    fun <T : Any> forEachRowOf(kClass: KClass<T>, params: Map<String, Any?>, action: (obj: T) -> Unit): DataResult<Unit>

    // --- Pomocnicze ---

    /** Zwraca string SQL bez wykonywania zapytania. */
    fun toSql(): String
}

fun TerminalReturningMethods.toList(vararg params: Pair<String, Any?>): DataResult<List<Map<String, Any?>>> =
    toList(params.toMap())

fun TerminalReturningMethods.toSingle(vararg params: Pair<String, Any?>): DataResult<Map<String, Any?>?> =
    toSingle(params.toMap())

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

fun TerminalReturningMethods.forEachRow(vararg params: Pair<String, Any?>, action: (row: Map<String, Any?>) -> Unit): DataResult<Unit> =
    forEachRow(params.toMap(), action)

inline fun <reified T: Any> TerminalReturningMethods.forEachRowOf(params: Map<String, Any?> = emptyMap(), noinline action: (obj: T) -> Unit): DataResult<Unit> =
    forEachRowOf(T::class, params, action)

inline fun <reified T: Any> TerminalReturningMethods.forEachRowOf(vararg params: Pair<String, Any?>, noinline action: (obj: T) -> Unit): DataResult<Unit> =
    forEachRowOf(T::class, params.toMap(), action)

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

    /** Tworzy TransactionStep z metodą toList */
    fun toList(params: Map<String, Any?> = emptyMap()): TransactionStep<List<Map<String, Any?>>>

    /** Tworzy TransactionStep z metodą toSingle */
    fun toSingle(params: Map<String, Any?> = emptyMap()): TransactionStep<Map<String, Any?>?>

    // --- Zwracanie obiektów data class ---

    /** Tworzy TransactionStep z metodą toListOf */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): TransactionStep<List<T>>

    /** Tworzy TransactionStep z metodą toSingleOf */
    fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): TransactionStep<T?>

    // --- Zwracanie wartości skalarnych ---

    /** Tworzy TransactionStep z metodą toField */
    fun <T : Any> toField(params: Map<String, Any?> = emptyMap()): TransactionStep<T?>

    /** Tworzy TransactionStep z metodą toColumn */
    fun <T : Any> toColumn(params: Map<String, Any?> = emptyMap()): TransactionStep<List<T?>>

    // --- Metoda modyfikująca ---

    /** Tworzy TransactionStep z metodą execute */
    fun execute(params: Map<String, Any?> = emptyMap()): TransactionStep<Int>
}

fun StepBuilderMethods.toList(vararg params: Pair<String, Any?>): TransactionStep<List<Map<String, Any?>>> =
    toList(params.toMap())

fun StepBuilderMethods.toSingle(vararg params: Pair<String, Any?>): TransactionStep<Map<String, Any?>?> =
    toSingle(params.toMap())

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