package org.octavius.data.builder

import kotlinx.coroutines.flow.Flow
import org.octavius.data.DataResult
import kotlin.reflect.KClass

interface StreamingTerminalMethods {
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

    fun toFlow(params: Map<String, Any?> = emptyMap()): Flow<Map<String, Any?>>

    fun <T : Any> toFlowOf(kClass: KClass<T>, params: Map<String, Any?> = emptyMap()): Flow<T>
}

fun StreamingTerminalMethods.forEachRow(vararg params: Pair<String, Any?>, action: (row: Map<String, Any?>) -> Unit): DataResult<Unit> =
    forEachRow(params.toMap(), action)

inline fun <reified T: Any> StreamingTerminalMethods.forEachRowOf(params: Map<String, Any?> = emptyMap(), noinline action: (obj: T) -> Unit): DataResult<Unit> =
    forEachRowOf(T::class, params, action)

inline fun <reified T: Any> StreamingTerminalMethods.forEachRowOf(vararg params: Pair<String, Any?>, noinline action: (obj: T) -> Unit): DataResult<Unit> =
    forEachRowOf(T::class, params.toMap(), action)

fun StreamingTerminalMethods.toFlow(vararg params: Pair<String, Any?>): Flow<Map<String, Any?>> =
    toFlow(params.toMap())

inline fun <reified T: Any> StreamingTerminalMethods.toFlowOf(params: Map<String, Any?> = emptyMap()): Flow<T> =
    toFlowOf(T::class, params.toMap())

inline fun <reified T: Any> StreamingTerminalMethods.toFlowOf(vararg params: Pair<String, Any?>): Flow<T> =
    toFlowOf(T::class, params.toMap())


