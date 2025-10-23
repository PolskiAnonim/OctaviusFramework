package org.octavius.data.builder

import kotlinx.coroutines.Job
import org.octavius.data.DataResult
import kotlin.reflect.KClass

/**
 * Interfejs dla buildera wykonującego zapytania asynchronicznie w podanym CoroutineScope.
 * Metody terminalne przyjmują callbacki i zwracają Job do kontroli cyklu życia.
 */
interface AsyncTerminalMethods {
    // --- Metody zwracające pełne wiersze ---

    /** Asynchronicznie pobiera listę wierszy i przekazuje wynik do callbacka onResult. */
    fun toList(
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<List<Map<String, Any?>>>) -> Unit
    ): Job

    /** Asynchronicznie pobiera pojedynczy wiersz i przekazuje wynik do callbacka onResult. */
    fun toSingle(
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<Map<String, Any?>?>) -> Unit
    ): Job

    // --- Metody zwracające obiekty data class ---

    /** Asynchronicznie mapuje wyniki na listę obiektów i przekazuje je do callbacka onResult. */
    fun <T : Any> toListOf(
        kClass: KClass<T>,
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<List<T>>) -> Unit
    ): Job

    /** Asynchronicznie mapuje wynik na pojedynczy obiekt i przekazuje go do callbacka onResult. */
    fun <T : Any> toSingleOf(
        kClass: KClass<T>,
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<T?>) -> Unit
    ): Job

    // --- Metody zwracające wartości skalarne (NOWO DODANE) ---

    /** Asynchronicznie pobiera pojedynczą wartość z pierwszej kolumny i przekazuje wynik do callbacka onResult. */
    fun <T : Any> toField(
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<T?>) -> Unit
    ): Job

    /** Asynchronicznie pobiera listę wartości z pierwszej kolumny i przekazuje wynik do callbacka onResult. */
    fun <T : Any> toColumn(
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<List<T?>>) -> Unit
    ): Job

    // --- Metoda modyfikująca ---

    /** Asynchronicznie wykonuje zapytanie modyfikujące i przekazuje wynik do callbacka onResult. */
    fun execute(
        params: Map<String, Any?> = emptyMap(),
        onResult: (DataResult<Int>) -> Unit
    ): Job
}

fun AsyncTerminalMethods.toList(
    vararg params: Pair<String, Any?>,
    onResult: (DataResult<List<Map<String, Any?>>>) -> Unit
): Job = toList(params.toMap(), onResult)

fun AsyncTerminalMethods.toSingle(
    vararg params: Pair<String, Any?>,
    onResult: (DataResult<Map<String, Any?>?>) -> Unit
): Job = toSingle(params.toMap(), onResult)

// Rozszerzenia inline
inline fun <reified T : Any> AsyncTerminalMethods.toListOf(
    params: Map<String, Any?> = emptyMap(),
    noinline onResult: (DataResult<List<T>>) -> Unit
): Job = toListOf(T::class, params, onResult)

inline fun <reified T : Any> AsyncTerminalMethods.toSingleOf(
    params: Map<String, Any?> = emptyMap(),
    noinline onResult: (DataResult<T?>) -> Unit
): Job = toSingleOf(T::class, params, onResult)

inline fun <reified T : Any> AsyncTerminalMethods.toListOf(
    vararg params: Pair<String, Any?>,
    noinline onResult: (DataResult<List<T>>) -> Unit
): Job = toListOf(T::class, params.toMap(), onResult)

inline fun <reified T : Any> AsyncTerminalMethods.toSingleOf(
    vararg params: Pair<String, Any?>,
    noinline onResult: (DataResult<T?>) -> Unit
): Job = toSingleOf(T::class, params.toMap(), onResult)

fun <T : Any> AsyncTerminalMethods.toField(
    vararg params: Pair<String, Any?>,
    onResult: (DataResult<T?>) -> Unit
): Job = toField(params.toMap(), onResult)

fun <T : Any> AsyncTerminalMethods.toColumn(
    vararg params: Pair<String, Any?>,
    onResult: (DataResult<List<T?>>) -> Unit
): Job = toColumn(params.toMap(), onResult)