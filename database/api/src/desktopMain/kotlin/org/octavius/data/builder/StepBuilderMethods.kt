package org.octavius.data.builder

import org.octavius.data.transaction.TransactionStep
import kotlin.reflect.KClass

/**
 * Interfejs dla StepBuilder - zawiera te same metody terminalne co TerminalReturningMethods
 * i TerminalModificationMethods, ale zwraca TransactionStep zamiast wykonywać zapytania.
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
    fun <T : Any> toField(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): TransactionStep<T?>

    /** Tworzy TransactionStep z metodą toColumn */
    fun <T : Any> toColumn(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): TransactionStep<List<T?>>

    // --- Metoda modyfikująca ---

    /** Tworzy TransactionStep z metodą execute */
    fun execute(params: Map<String, Any?> = emptyMap()): TransactionStep<Int>
}

fun StepBuilderMethods.toList(vararg params: Pair<String, Any?>): TransactionStep<List<Map<String, Any?>>> =
    toList(params.toMap())

fun StepBuilderMethods.toSingle(vararg params: Pair<String, Any?>): TransactionStep<Map<String, Any?>?> =
    toSingle(params.toMap())

inline fun <reified T : Any> StepBuilderMethods.toField(
    params: Map<String, Any?> = emptyMap()
): TransactionStep<T?> = toField(T::class, params)

inline fun <reified T : Any> StepBuilderMethods.toField(
    vararg params: Pair<String, Any?>
): TransactionStep<T?> = toField(T::class, params.toMap())

inline fun <reified T : Any> StepBuilderMethods.toColumn(
    params: Map<String, Any?> = emptyMap()
): TransactionStep<List<T?>> = toColumn(T::class, params)

inline fun <reified T : Any> StepBuilderMethods.toColumn(
    vararg params: Pair<String, Any?>
): TransactionStep<List<T?>> = toColumn(T::class, params.toMap())

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
