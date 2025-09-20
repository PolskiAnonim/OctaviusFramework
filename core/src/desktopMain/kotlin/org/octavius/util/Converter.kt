package org.octavius.util

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
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

    // Mapowanie właściwości w klasie po nazwie dla łatwego dostępu
    val propertiesByName = kClass.memberProperties.associateBy { it.name }

    val args = constructor.parameters.mapNotNull { param ->
        // Właściwość odpowiadająca parametrowi konstruktora
        val property = propertiesByName[param.name]
            ?: throw IllegalStateException("Błąd wewnętrzny: Nie znaleziono właściwości dla parametru ${param.name}")

        // Określ klucz, którego szukamy w mapie
        val keyName = property.findAnnotation<MapKey>()?.name
            ?: Converters.toSnakeCase(param.name!!)

        // Sprawdzamy, czy klucz ISTNIEJE w mapie (nie tylko czy wartość nie jest null)
        val hasKeyInMap = this.containsKey(keyName)

        when {
            // Przypadek 1: Klucz istnieje w mapie. Zawsze używamy wartości z mapy,
            // nawet jeśli jest to jawne null.
            hasKeyInMap -> {
                param to this[keyName]
            }

            // Przypadek 2: Klucz NIE istnieje w mapie.
            // Sprawdzamy, czy parametr ma wartość domyślną.
            param.isOptional -> {
                // Parametr ma wartość domyślną, więc pomijamy go w mapie args.
                // callBy() automatycznie użyje wartości domyślnej.
                null // mapNotNull usunie tę parę
            }

            // Parametr nie ma wartości domyślnej i nie ma go w mapie.
            // Musimy zdecydować, co wstawić.
            param.type.isMarkedNullable -> {
                // Parametr jest nullable (np. String?), więc możemy wstawić null.
                param to null
            }

            else -> {
                // Parametr jest non-nullable (np. String) i nie ma wartości domyślnej,
                // a klucz nie został znaleziony w mapie. To jest błąd.
                throw IllegalArgumentException(
                    "Wymagany parametr '${param.name}' dla klasy ${kClass.simpleName} " +
                            "nie został znaleziony w mapie i nie posiada wartości domyślnej."
                )
            }
        }
    }.associate { (k, v) -> k to v }

    // Wywołaj główny konstruktor z przygotowanymi argumentami.
    return constructor.callBy(args)
}


// --- Konwersja Z OBIEKTU do MAPY (dwie oddzielne funkcje) ---

/**
 * Konwertuje obiekt na mapę `Map<String, Any?>`, stosując konwencję camelCase -> snake_case.
 * Używa adnotacji @MapKey do nadpisania domyślnej konwencji nazewnictwa.
 *
 * @receiver Obiekt do skonwertowania.
 * @return Niemutowalna mapa z kluczami w formacie snake_case.
 */
fun <T : Any> T.toMap(): Map<String, Any?> {
    return this::class.memberProperties.associate { property ->
        // Określ klucz:
        // 1. Użyj nazwy z adnotacji @MapKey, jeśli istnieje.
        // 2. W przeciwnym razie, użyj konwencji: przekonwertuj nazwę właściwości na snake_case.
        val keyName = property.findAnnotation<MapKey>()?.name
            ?: Converters.toSnakeCase(property.name)

        val value = property.getter.call(this)
        keyName to value
    }
}