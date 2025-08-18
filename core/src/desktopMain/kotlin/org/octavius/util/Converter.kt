package org.octavius.util

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Adnotacja używana do określenia niestandardowego klucza dla właściwości
 * podczas konwersji obiektu do/z mapy.
 *
 * Domyślnie używana jest nazwa właściwości. Ta adnotacja pozwala ją nadpisać,
 * co jest przydatne, gdy nazwy kluczy z mapy nie powinne odpowiadać nazwom właściwości
 * np CamelCase vs snake_case
 *
 * @property name Nazwa klucza, która zostanie użyta w mapie.
 *
 * @see toDataObject
 * @see toMap
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MapKey(val name: String)

// --- Konwersja Z MAPY do OBIEKTU  ---

inline fun <reified T : Any> Map<String, Any?>.toDataObject(): T {
    return toDataObject(T::class)
}

/**
 * Wewnętrzna implementacja konwersji mapy na obiekt, operująca na [KClass].
 *
 *
 * @param kClass Klasa docelowego obiektu.
 * @return Nowa instancja klasy [T] wypełniona danymi z mapy.
 */
fun <T : Any> Map<String, Any?>.toDataObject(kClass: KClass<T>): T {
    val constructor = kClass.primaryConstructor
        ?: throw IllegalArgumentException("Klasa ${kClass.simpleName} musi mieć główny konstruktor.")

    // Mapowanie nazw parametrów konstruktora na ich właściwości w klasie
    val propertiesByName = kClass.memberProperties.associateBy { it.name }

    val args = constructor.parameters.associateWith { param ->
        val property = propertiesByName[param.name]
            ?: throw IllegalStateException("Nie znaleziono właściwości dla parametru konstruktora: ${param.name}")

        // Użyj nazwy z adnotacji @MapKey, jeśli istnieje, w przeciwnym razie użyj nazwy parametru.
        val keyName = property.findAnnotation<MapKey>()?.name ?: param.name
        val value = this[keyName] // Pobierz wartość z mapy

        value
    }

    // Wywołaj główny konstruktor z przygotowanymi argumentami.
    return constructor.callBy(args)
}


// --- Konwersja Z OBIEKTU do MAPY (dwie oddzielne funkcje) ---

/**
 * Konwertuje obiekt na mapę `Map<String, Any?>`.
 * Używa adnotacji @MapKey do określenia nazwy klucza w mapie.
 *
 * @receiver Obiekt do skonwertowania.
 * @return Niemutowalna mapa reprezentująca stan obiektu.
 */
fun <T : Any> T.toMap(): Map<String, Any?> {
    return this::class.memberProperties.associate { property ->
        // Określ klucz: użyj nazwy z adnotacji @MapKey lub nazwy właściwości
        val keyName = property.findAnnotation<MapKey>()?.name ?: property.name

        // Pobierz wartość właściwości
        val value = property.getter.call(this)

        // Stwórz parę (Klucz, Wartość), z której `associate` zbuduje mapę
        keyName to value
    }
}