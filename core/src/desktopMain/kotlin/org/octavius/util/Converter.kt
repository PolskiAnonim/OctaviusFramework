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
 * Adnotacja określająca, że zagnieżdżona data class nie powinna być spłaszczana
 * podczas tworzenia mapy kompozytowej.
 *
 * Zamiast tego, cała instancja obiektu zostanie wstawiona jako wartość do mapy,
 * co jest przydatne przy współpracy z warstwami persystencji obsługującymi
 * typy złożone (Composite User Types).
 *
 * @see toCompositeValueMap
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NonFlattenable()


// --- Konwersja Z MAPY do OBIEKTU  ---

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
        val property = propertiesByName[param.name]
            ?: throw IllegalStateException("Nie znaleziono właściwości dla parametru konstruktora: ${param.name}")

        // Użyj nazwy z adnotacji @MapKey, jeśli istnieje, w przeciwnym razie użyj nazwy parametru.
        val keyName = property.findAnnotation<MapKey>()?.name ?: param.name
        val value = this[keyName] // Pobierz wartość z mapy

        val paramClass = param.type.classifier as? KClass<*>

        // Sprawdzenie, czy parametr konstruktora jest zagnieżdżoną data classą
        if (paramClass?.isData == true) {
            // 1. Sprawdź, czy wartość jest już gotowym obiektem właściwego typu
            if (paramClass.isInstance(value)) {
                value // Jeśli tak, po prostu go użyj
            } else {
                // 2. Jeśli nie, zbuduj go z płaskiej mapy
                this.toDataObject(paramClass)
            }
        } else {
            // Dla typów prostych nic się nie zmienia
            value
        }
    }

    // Wywołaj główny konstruktor z przygotowanymi argumentami.
    return constructor.callBy(args)
}


// --- Konwersja Z OBIEKTU do MAPY (dwie oddzielne funkcje) ---

/**
 * Konwertuje obiekt na **PŁASKĄ** mapę `Map<String, Any?>`.
 *
 * Ta funkcja zawsze spłaszcza całą hierarchię zagnieżdżonych data class,
 * tworząc jedną mapę z kluczami i wartościami prostymi.
 * Adnotacja @NonFlattenable jest ignorowana.
 *
 * Idealna do serializacji obiektu do formatu, który nie rozumie zagnieżdżeń.
 *
 * Przykład:
 * ```
 * data class Address(val city: String)
 * data class User(val id: Int, val address: Address)
 *
 * val user = User(1, Address("New York"))
 * user.toFlatValueMap() // Wynik: mapOf("id" to 1, "city" to "New York")
 * ```
 *
 * @receiver Obiekt do skonwertowania.
 * @return Niemutowalna, płaska mapa reprezentująca stan obiektu.
 */
fun <T : Any> T.toFlatValueMap(): Map<String, Any?> {
    return this::class.memberProperties.fold(mutableMapOf<String, Any?>()) { accumulator, property ->
        val value = property.getter.call(this)

        // Jeśli wartość to data class i nie jest nullem, spłaszcz ją rekurencyjnie
        if (value != null && (property.returnType.classifier as? KClass<*>)?.isData == true) {
            accumulator.putAll(value.toFlatValueMap())
        } else {
            // W przeciwnym razie dodaj jako prostą wartość
            val keyName = property.findAnnotation<MapKey>()?.name ?: property.name
            accumulator[keyName] = value
        }
        accumulator
    }.toMap()
}

/**
 * Konwertuje obiekt na mapę `Map<String, Any?>`.
 *
 * Ta funkcja domyślnie spłaszcza zagnieżdżone data classy, ale jeśli właściwość
 * jest oznaczona adnotacją [@NonFlattenable], to zagnieżdżony obiekt
 * zostanie wstawiony do mapy **jako instancja**, a nie zmapowany.
 *
 * Idealna do przygotowania obiektu do zapisu w systemie, który obsługuje
 * typy złożone (Composite Types).
 *
 * Przykład:
 * ```
 * data class Address(val city: String)
 * data class User(val id: Int, @NonFlattenable val address: Address)
 *
 * val user = User(1, Address("New York"))
 * user.toCompositeValueMap() // Wynik: mapOf("id" to 1, "address" to Address("New York"))
 * ```
 *
 * @receiver Obiekt do skonwertowania.
 * @return Niemutowalna mapa z wartościami prostymi lub zagnieżdżonymi obiektami.
 */
fun <T : Any> T.toCompositeValueMap(): Map<String, Any?> {
    return this::class.memberProperties.fold(mutableMapOf<String, Any?>()) { accumulator, property ->
        val value = property.getter.call(this)
        val keyName = property.findAnnotation<MapKey>()?.name ?: property.name

        // Sprawdź, czy to data class i czy ma adnotację @NonFlattenable
        if (value != null && (property.returnType.classifier as? KClass<*>)?.isData == true) {
            if (property.findAnnotation<NonFlattenable>() != null) {
                // Adnotacja jest -> wstaw obiekt bezpośrednio do mapy
                accumulator[keyName] = value
            } else {
                // Adnotacji nie ma -> spłaszcz rekurencyjnie
                accumulator.putAll(value.toCompositeValueMap())
            }
        } else {
            // To prosta wartość lub null, wstaw ją bezpośrednio
            accumulator[keyName] = value
        }
        accumulator
    }.toMap()
}