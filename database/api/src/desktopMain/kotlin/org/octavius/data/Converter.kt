package org.octavius.data

import org.octavius.data.exception.ConversionException
import org.octavius.data.exception.ConversionExceptionMessage
import org.octavius.data.util.toSnakeCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Adnotacja używana do określenia niestandardowego klucza dla właściwości
 * podczas konwersji obiektu do/z mapy.
 *
 * Domyślnie używana jest nazwa właściwości z zamianą snake_case <-> camelCase. Ta adnotacja pozwala ją nadpisać,
 * co jest przydatne, gdy nazwy kluczy z mapy nie powinne odpowiadać nazwom właściwości
 * np userId vs user
 *
 * @property name Nazwa klucza, która zostanie użyta w mapie.
 *
 * @see toDataObject
 * @see toMap
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MapKey(val name: String)

// --- Wspólny Cache i Metadane dla obu konwersji ---

/**
 * Przechowuje kompletne, pre-obliczone metadane dla parametru konstruktora.
 * Zapewnia wydajny dostęp bez potrzeby ponownej refleksji.
 */
private data class ConstructorParamMetadata<T : Any>(
    val parameter: KParameter,
    val property: KProperty1<T, Any?>,
    val type: KType,
    val keyName: String
)

/**
 * Przechowuje metadane klasy oparte na jej głównym konstruktorze.
 * Służy jako centralny cache dla operacji `toDataObject` i `toMap`.
 */
private data class DataObjectClassMetadata<T : Any>(
    val constructor: KFunction<T>,
    val constructorProperties: List<ConstructorParamMetadata<T>>
)

// Używamy jednego, wspólnego cache'a dla obu operacji.
private val dataObjectCache = ConcurrentHashMap<KClass<*>, DataObjectClassMetadata<*>>()

/**
 * Wewnętrzna funkcja do pobierania lub tworzenia metadanych dla danej klasy.
 * To jest jedyne miejsce, gdzie odbywa się kosztowna refleksja. Wyniki są cachowane.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Any> getOrCreateDataObjectMetadata(kClass: KClass<T>): DataObjectClassMetadata<T> {
    return dataObjectCache.getOrPut(kClass) {
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("Klasa ${kClass.simpleName} musi mieć główny konstruktor.")

        // Mapowanie właściwości w klasie po nazwie dla łatwego dostępu
        val propertiesByName = kClass.memberProperties.associateBy { it.name }

        val constructorProperties = constructor.parameters.map { param ->
            val property = propertiesByName[param.name]!!

            val keyName = property.findAnnotation<MapKey>()?.name
                ?: param.name!!.toSnakeCase()

            ConstructorParamMetadata(
                parameter = param,
                property = property,
                type = param.type,
                keyName = keyName
            )
        }
        DataObjectClassMetadata(constructor, constructorProperties)
    } as DataObjectClassMetadata<T>
}

// --- Konwersja Z MAPY do OBIEKTU  ---

inline fun <reified T : Any> Map<String, Any?>.toDataObject(): T {
    return toDataObject(T::class)
}

fun <T : Any> Map<String, Any?>.toDataObject(kClass: KClass<T>): T {
    val metadata = getOrCreateDataObjectMetadata(kClass)

    val args = metadata.constructorProperties.mapNotNull { meta ->
        val (param, _, type, keyName) = meta

        val valueToUse = when {
            // Przypadek 1: Klucz istnieje w mapie. Zawsze używamy wartości z mapy,
            // nawet jeśli jest to jawne null.
            this.containsKey(keyName) -> this[keyName]
            // Przypadek 2: Klucz NIE istnieje w mapie.
            // Parametr ma wartość domyślną, więc pomijamy go w mapie args.
            // callBy() automatycznie użyje wartości domyślnej.
            // mapNotNull usunie tę parę
            param.isOptional -> return@mapNotNull null
            // Przypadek 3: Parametr nie ma wartości domyślnej i nie ma go w mapie.
            // Parametr jest nullable (np. String?), więc możemy wstawić null.
            param.type.isMarkedNullable -> null
            // Przypadek 4: Parametr jest non-nullable (np. String) i nie ma wartości domyślnej,
            // a klucz nie został znaleziony w mapie. To jest błąd.
            else -> throw ConversionException(
                messageEnum = ConversionExceptionMessage.MISSING_REQUIRED_PROPERTY,
                targetType = kClass.qualifiedName,
                value = keyName,
                rowData = this,
                propertyName = param.name
            )
        }

        // Walidacja z użyciem cachowanego KType
        val validatedValue = try {
            validateAndCast(valueToUse, type)
        } catch (e: ConversionException) {
            throw ConversionException(
                messageEnum = e.messageEnum,
                value = e.value,
                targetType = e.targetType,
                rowData = this,
                propertyName = param.name
            )
        }

        param to validatedValue
    }.associate { it }

    // Wywołaj główny konstruktor z przygotowanymi argumentami.
    try {
        return metadata.constructor.callBy(args)
    } catch (e: Exception) {
        throw ConversionException(
            messageEnum = ConversionExceptionMessage.OBJECT_MAPPING_FAILED,
            targetType = kClass.qualifiedName ?: kClass.simpleName ?: "unknown",
            rowData = this,
            cause = e
        )
    }
}


// --- Konwersja Z OBIEKTU do MAPY --

/**
 * Konwertuje obiekt data class na mapę, gdzie kluczami są nazwy właściwości
 * (lub wartości z adnotacji @MapKey), a wartościami są wartości tych właściwości.
 *
 * @param includeNulls Jeśli `true` (domyślnie), właściwości z wartością `null` zostaną
 *                     uwzględnione w mapie. Jeśli `false`, zostaną pominięte.
 *                     Jest to przydatne np. przy operacjach UPDATE, gdzie chcemy
 *                     zmienić tylko pola, które nie są nullem.
 * @return Mapa reprezentująca obiekt.
 */
fun <T : Any> T.toMap(includeNulls: Boolean = true): Map<String, Any?> {
    val metadata = getOrCreateDataObjectMetadata(this::class) as DataObjectClassMetadata<T>

    return metadata.constructorProperties.mapNotNull { meta ->
        val (_, property, _, keyName) = meta

        val value = property.get(this)
        if (!includeNulls && value == null) {
            null
        } else {
            keyName to value
        }
    }.associate { it }
}
