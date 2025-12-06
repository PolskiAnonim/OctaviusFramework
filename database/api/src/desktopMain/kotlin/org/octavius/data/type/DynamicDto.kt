package org.octavius.data.type

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.octavius.data.annotation.DynamicallyMappable
import org.octavius.data.annotation.PgComposite
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
@PgComposite(name = "dynamic_dto")
data class DynamicDto private constructor(
    val typeName: String,
    val dataPayload: JsonElement
) {
    companion object {
        /**
         * Wygodna metoda fabryczna do tworzenia instancji [DynamicDto] z obiektu domenowego.
         *
         * Jest to preferowana ścieżka dla użytkownika końcowego. Używa refleksji do
         * automatycznego znalezienia `typeName` z adnotacji [DynamicallyMappable]
         * i serializuje obiekt do [JsonElement].
         *
         * @param value Instancja obiektu do opakowania. Musi posiadać adnotacje
         *              [DynamicallyMappable] oraz `@Serializable`.
         * @return Nowa, w pełni skonstruowana instancja [DynamicDto].
         * @throws ConversionException jeśli klasa obiektu nie ma wymaganej adnotacji
         *                           lub jeśli wystąpi błąd podczas serializacji JSON.
         */
        inline fun <reified T: Any> from(value: T): DynamicDto {
            @Suppress("UNCHECKED_CAST")
            val kClass = value::class as KClass<Any>

            // 1. Znajdź nazwę typu (refleksja)
            val annotation = kClass.findAnnotation<DynamicallyMappable>()
                ?: throw ConversionException(
                    messageEnum = ConversionExceptionMessage.JSON_SERIALIZATION_FAILED,
                    value = kClass.simpleName,
                    targetType = DynamicallyMappable::class.simpleName
                )

            // 2. Znajdź serializer
            val serializer = try {
                // Udajemy że jest to bezpieczniejsze od innych metod (i tak odczyt nie pozwoli na pełną informację)
                serializer<T>()
            } catch (e: Exception) {
                throw ConversionException(
                    messageEnum = ConversionExceptionMessage.JSON_SERIALIZATION_FAILED,
                    targetType = annotation.typeName,
                    cause = e
                )
            }

            // 3. Deleguj do wersji zoptymalizowanej
            @Suppress("UNCHECKED_CAST")
            return from(value, annotation.typeName, serializer as KSerializer<Any>)
        }

        /**
         * [ŚCIEŻKA FRAMEWORKA]
         * Tworzy DTO używając dostarczonego z zewnątrz (zcache'owanego) serializera.
         * Zero refleksji, maksymalna wydajność.
         */
        fun from(value: Any, typeName: String, serializer: KSerializer<Any>): DynamicDto {
            try {
                // Serializacja do JsonElement
                val jsonPayload = Json.encodeToJsonElement(serializer, value)

                return DynamicDto(typeName, jsonPayload)
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