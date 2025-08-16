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
 * @see toFlatValueMap
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MapKey(val name: String)

/**
 * Konwertuje płaską mapę `Map<String, Any?>` na obiekt typu [T].
 *
 * Ta funkcja wykorzystuje refleksję do zmapowania kluczy z mapy na parametry
 * głównego konstruktora klasy [T]. Obsługuje również zagnieżdżone data classy,
 * rekonstruując całą hierarchię obiektów z jednej, płaskiej mapy.
 *
 * Właściwości w klasie docelowej mogą być oznaczone adnotacją [MapKey],
 * aby określić niestandardową nazwę klucza w mapie.
 *
 * Przykład użycia:
 * ```
 * val map = mapOf("id" to 1, "user_name" to "John", "city" to "New York")
 * val user: User = map.toDataObject()
 * ```
 *
 * @receiver Mapa, z której tworzony jest obiekt.
 * @return Nowa instancja klasy [T] wypełniona danymi z mapy.
 * @throws IllegalArgumentException jeśli klasa [T] nie ma głównego konstruktora.
 * @throws IllegalStateException jeśli nie można znaleźć właściwości dla parametru konstruktora.
 */
inline fun <reified T : Any> Map<String, Any?>.toDataObject(): T {
    return toDataObject(T::class)
}

/**
 * Wewnętrzna implementacja konwersji mapy na obiekt, operująca na [KClass].
 *
 * Ta funkcja wykonuje główną logikę mapowania i obsługuje rekurencję dla
 * zagnieżdżonych data class. Jest to mniej wygodna wersja, przeznaczona
 * do użytku wewnętrznego przez jej odpowiednik `reified`.
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
        val paramClass = param.type.classifier as? KClass<*>

        // Sprawdzenie, czy parametr konstruktora jest zagnieżdżoną data classą
        if (paramClass?.isData == true) {
            // Jeśli tak, wywołaj rekurencyjnie konwersję dla tego typu,
            // przekazując całą, płaską mapę.
            this.toDataObject(paramClass)
        } else {
            // W przeciwnym razie, to proste pole. Znajdź jego wartość w mapie.
            val property = propertiesByName[param.name]
                ?: throw IllegalStateException("Nie znaleziono właściwości dla parametru konstruktora: ${param.name}")

            // Użyj nazwy z adnotacji @MapKey, jeśli istnieje, w przeciwnym razie użyj nazwy parametru.
            val keyName = property.findAnnotation<MapKey>()?.name ?: param.name
            this[keyName]
        }
    }

    // Wywołaj główny konstruktor z przygotowanymi argumentami.
    return constructor.callBy(args)
}

/**
 * Konwertuje obiekt (potencjalnie zagnieżdżony) na **płaską** mapę `Map<String, Any?>`.
 *
 * Funkcja przechodzi przez wszystkie właściwości obiektu. Jeśli właściwość jest
 * zagnieżdżoną data classą, jej zawartość jest rekurencyjnie "wciągana"
 * do mapy nadrzędnej. Dzięki temu cała hierarchia obiektu jest reprezentowana
 * jako jedna, płaska struktura klucz-wartość.
 *
 * Właściwości mogą być oznaczone adnotacją [MapKey] w celu zdefiniowania
 * niestandardowych kluczy w wynikowej mapie.
 *
 * Przykład użycia:
 * ```
 * data class Address(val city: String)
 * data class User(val id: Int, @MapKey("user_name") val name: String, val address: Address)
 *
 * val user = User(1, "John", Address("New York"))
 * val map = user.toFlatValueMap()
 * // Wynik: mapOf("id" to 1, "user_name" to "John", "city" to "New York")
 * ```
 *
 * @receiver Obiekt do skonwertowania.
 * @return Niemutowalna, płaska mapa reprezentująca stan obiektu.
 */
fun <T : Any> T.toFlatValueMap(): Map<String, Any?> {
    return this::class.memberProperties.fold(mutableMapOf<String, Any?>()) { accumulator, property ->
        val value = property.getter.call(this)
        val valueClass = property.returnType.classifier as? KClass<*>

        // Sprawdzenie, czy właściwość jest zagnieżdżoną data classą
        if (value != null && valueClass?.isData == true) {
            // Jeśli tak, dołącz jej spłaszczoną mapę do akumulatora.
            accumulator.putAll(value.toFlatValueMap())
        } else {
            // W przeciwnym razie, to proste pole. Dodaj je bezpośrednio do mapy.
            val keyName = property.findAnnotation<MapKey>()?.name ?: property.name
            accumulator[keyName] = value
        }
        accumulator
    }.toMap() // Zwróć jako niemutowalną mapę.
}