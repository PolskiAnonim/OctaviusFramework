package org.octavius.data.type

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.octavius.data.annotation.DynamicallyMappable
import org.octavius.data.annotation.PgType
import org.octavius.data.exception.ConversionException
import org.octavius.data.exception.ConversionExceptionMessage
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Reprezentuje polimorficzny obiekt gotowy do zapisu w bazie danych.
 *
 * Ta klasa jest publicznym API i "kontenerem transportowym", który jednoznacznie
 * opakowuje dowolny obiekt oznaczony adnotacją [DynamicallyMappable] w strukturę
 * zrozumiałą dla frameworka i bazy danych PostgreSQL. Odpowiada typowi
 * `dynamic_dto` w bazie.
 *
 * Służy jako jawny, kontrolowany sposób na przygotowanie polimorficznych danych do zapisu,
 * stanowiąc alternatywę dla w pełni automatycznego mechanizmu konwersji frameworka.
 *
 * **Asymetria Zapisu i Odczytu:**
 * - **Przy zapisie:** Używasz tej klasy (lub jej fabryki `from()`), aby "zapakować"
 *   swój obiekt domenowy.
 * - **Przy odczycie:** Framework automatycznie "rozpakowuje" dane i zwraca
 *   bezpośrednio obiekt domenowy (np. `DynamicProfile`), a nie instancję `DynamicDto`.
 *
 * @property typeName Klucz identyfikujący typ obiektu, pobierany z adnotacji [DynamicallyMappable].
 * @property dataPayload Obiekt zserializowany do postaci [JsonObject].
 * @see DynamicallyMappable
 */
@ConsistentCopyVisibility
@PgType(name = "dynamic_dto")
data class DynamicDto private constructor(
    val typeName: String,
    val dataPayload: JsonObject
) {
    companion object {
        /**
         * Wygodna metoda fabryczna do tworzenia instancji [DynamicDto] z obiektu domenowego.
         *
         * Jest to preferowana ścieżka dla użytkownika końcowego. Używa refleksji do
         * automatycznego znalezienia `typeName` z adnotacji [DynamicallyMappable]
         * i serializuje obiekt do [JsonObject].
         *
         * @param value Instancja obiektu do opakowania. Musi posiadać adnotacje
         *              [DynamicallyMappable] oraz `@Serializable`.
         * @return Nowa, w pełni skonstruowana instancja [DynamicDto].
         * @throws ConversionException jeśli klasa obiektu nie ma wymaganej adnotacji
         *                           lub jeśli wystąpi błąd podczas serializacji JSON.
         */
        fun from(value: Any): DynamicDto {
            @Suppress("UNCHECKED_CAST")
            val kClass = value::class as KClass<Any>
            val annotation = kClass.findAnnotation<DynamicallyMappable>()
                ?: throw ConversionException(
                    messageEnum = ConversionExceptionMessage.JSON_SERIALIZATION_FAILED,
                    value = kClass.simpleName,
                    targetType = DynamicallyMappable::class.simpleName
                )

            // Deleguje do bardziej jawnej wersji, aby nie powielać logiki serializacji
            return from(value, annotation.typeName)
        }

        /**
         * Jawna i zoptymalizowana metoda fabryczna, przeznaczona dla frameworka i zaawansowanych
         * scenariuszy użycia.
         *
         * Przyjmuje `typeName` jako jawny parametr, całkowicie omijając potrzebę refleksji
         * w celu znalezienia adnotacji. Jest to ścieżka o najwyższej wydajności.
         *
         * @param value Instancja obiektu do opakowania. Musi posiadać adnotację `@Serializable`.
         * @param typeName Klucz identyfikujący typ, który normalnie pochodziłby z adnotacji.
         * @return Nowa, w pełni skonstruowana instancja [DynamicDto].
         * @throws ConversionException jeśli wystąpi błąd podczas serializacji JSON.
         */
        @OptIn(InternalSerializationApi::class)
        fun from(value: Any, typeName: String): DynamicDto {
            try {
                @Suppress("UNCHECKED_CAST")
                val kClass = value::class as KClass<Any>
                val serializer = kClass.serializer()
                val jsonElement = Json.encodeToJsonElement(serializer, value)

                if (jsonElement !is JsonObject) {
                    throw IllegalStateException(
                        "Serialization of '${kClass.simpleName}' did not result in a JsonObject. " +
                                "Only class-based serialization is supported for DynamicDto."
                    )
                }

                return DynamicDto(
                    typeName = typeName,
                    dataPayload = jsonElement
                )
            } catch (e: Exception) {
                throw ConversionException(
                    messageEnum = ConversionExceptionMessage.JSON_SERIALIZATION_FAILED,
                    targetType = typeName,
                    cause = e
                )
            }
        }
    }
}