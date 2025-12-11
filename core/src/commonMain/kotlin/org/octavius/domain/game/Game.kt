package org.octavius.domain.game

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.octavius.data.annotation.DynamicallyMappable
import org.octavius.data.annotation.PgEnum
import org.octavius.data.util.CaseConvention
import org.octavius.data.util.CaseConverter
import org.octavius.data.util.toSnakeCase
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T

@PgEnum
@Serializable(with = GameStatusSerializer::class)
enum class GameStatus : EnumWithFormatter<GameStatus> {
    NotPlaying,
    WithoutTheEnd,
    Played,
    ToPlay,
    Playing;

    override fun toDisplayString(): String {
        return when (this) {
            NotPlaying -> T.get("games.status.notPlaying")
            WithoutTheEnd -> T.get("games.status.endless")
            Played -> T.get("games.status.played")
            ToPlay -> T.get("games.status.toPlay")
            Playing -> T.get("games.status.playing")
        }
    }
}

object GameStatusSerializer : KSerializer<GameStatus> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GameStatus", PrimitiveKind.STRING)

    // Jak zamienić enum na string do zapisu w JSON
    override fun serialize(encoder: Encoder, value: GameStatus) {
        encoder.encodeString(CaseConverter.convert(value.name, CaseConvention.PASCAL_CASE, CaseConvention.SNAKE_CASE_UPPER))
    }

    // Jak zamienić string z JSON-a na enum
    override fun deserialize(decoder: Decoder): GameStatus {
        val string = decoder.decodeString()
        val convertedString = CaseConverter.convert(string, CaseConvention.SNAKE_CASE_UPPER, CaseConvention.PASCAL_CASE)
        return GameStatus.entries.firstOrNull { it.name == convertedString }
            ?: throw SerializationException("Unknown GameStatus: $string")
    }
}
