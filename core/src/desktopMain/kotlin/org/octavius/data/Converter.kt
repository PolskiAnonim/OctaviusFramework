package org.octavius.data

import org.octavius.util.Converters
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
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

// --- Cache dla toDataObject() ---

// Przechowujemy główny konstruktor i szczegóły parametrów.
private data class ToDataObjectClassMetadata<T : Any>(
    val constructor: KFunction<T>,
    // Triple: (parametr konstruktora, odpowiadająca mu właściwość, nazwa klucza w mapie)
    val parameterDetails: List<Triple<KParameter, KProperty1<T, Any?>, String>>
)

// Używamy KClass<*> jako klucza, aby być generycznym i unikać problemów z typami.
private val toDataObjectCache = ConcurrentHashMap<KClass<*>, ToDataObjectClassMetadata<*>>()


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
    // Pobierz lub oblicz metadane dla tej klasy
    @Suppress("UNCHECKED_CAST")
    val metadata = toDataObjectCache.getOrPut(kClass) {
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("Klasa ${kClass.simpleName} musi mieć główny konstruktor.")

        // Mapowanie właściwości w klasie po nazwie dla łatwego dostępu
        val propertiesByName = kClass.memberProperties.associateBy { it.name }

        val parameterDetails = constructor.parameters.map { param ->
            val property = propertiesByName[param.name]
                ?: throw IllegalStateException("Błąd wewnętrzny: Nie znaleziono właściwości dla parametru ${param.name}")

            val keyName = property.findAnnotation<MapKey>()?.name
                ?: Converters.toSnakeCase(param.name!!)

            Triple(param, property, keyName)
        }
        ToDataObjectClassMetadata(constructor, parameterDetails)
    } as ToDataObjectClassMetadata<T>


    val args = metadata.parameterDetails.mapNotNull { (param, property, keyName) ->
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
    return metadata.constructor.callBy(args)
}

// --- Cache dla toMap() ---

// Przechowujemy pre-obliczone nazwy kluczy mapy i referencje do właściwości.
private data class ToMapClassMetadata<T : Any>(
    val propertiesMapInfo: List<Pair<String, KProperty1<T, Any?>>>
)

// Używamy KClass<*> jako klucza, aby być generycznym i unikać problemów z typami.
private val toMapCache = ConcurrentHashMap<KClass<*>, ToMapClassMetadata<*>>()


// --- Konwersja Z OBIEKTU do MAPY ---

fun <T : Any> T.toMap(): Map<String, Any?> {
    // Pobierz lub oblicz metadane dla tej klasy
    // Bezpieczne rzutowanie jest możliwe, ponieważ kluczem w mapie jest T::class, a wartością ClassMetadata<T>
    @Suppress("UNCHECKED_CAST")
    val metadata = toMapCache.getOrPut(this::class) {
        val propertiesMapInfo = this::class.memberProperties.map { property ->
            val keyName = property.findAnnotation<MapKey>()?.name
                ?: Converters.toSnakeCase(property.name)
            keyName to (property as KProperty1<T, Any?>) // Rzutowanie na konkretny typ, żeby pasowało do T
        }
        ToMapClassMetadata(propertiesMapInfo)
    } as ToMapClassMetadata<T>

    return metadata.propertiesMapInfo.associate { (keyName, property) ->
        // Ta część MUSI być wywołana dla każdej instancji, ponieważ wartości są dynamiczne
        keyName to property.getter.call(this)
    }
}