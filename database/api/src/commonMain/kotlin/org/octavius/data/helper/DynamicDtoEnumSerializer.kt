package org.octavius.data.helper

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.octavius.data.util.CaseConvention
import org.octavius.data.util.CaseConverter

/**
 * Serializator dla użycia w enumie dla DynamicDto
 * Należy stworzyć object który z niego dziedziczy i prekazać dla kotlinx.serialization
 */
open class DynamicDtoEnumSerializer<E : Enum<E>>(
    serialName: String,
    private val entries: List<E>, // Przekazujemy listę wartości
    // Jak zapisane są wartości w bazie PostgreSQL (np. 'PENDING', 'in_progress')
    private val pgConvention: CaseConvention = CaseConvention.SNAKE_CASE_UPPER,
    // Jak zapisane są wartości w klasie Enum w Kotlinie (np. Pending, InProgress)
    private val kotlinConvention: CaseConvention = CaseConvention.PASCAL_CASE
) : KSerializer<E> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: E) {
        val pgName = CaseConverter.convert(
            value.name,
            kotlinConvention,
            pgConvention
        )
        encoder.encodeString(pgName)
    }

    override fun deserialize(decoder: Decoder): E {
        val string = decoder.decodeString()

        // Logika odwracania też jest tu RAZ
        // Uwaga: Można to zoptymalizować mapą, jeśli wydajność jest krytyczna,
        // ale przy enumach to zazwyczaj pomijalne.
        val kotlinName = CaseConverter.convert(
            string,
            pgConvention,
            kotlinConvention
        )

        return entries.firstOrNull { it.name == kotlinName }
            ?: throw SerializationException("Unknown $descriptor name: $string")
    }
}