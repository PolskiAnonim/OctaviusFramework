package org.octavius.domain.game

import kotlinx.serialization.Serializable
import org.octavius.data.annotation.PgEnum
import org.octavius.data.helper.DynamicDtoEnumSerializer
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T

@PgEnum
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

object GameStatusDynamicDtoSerializer : DynamicDtoEnumSerializer<GameStatus>(
    "GameStatus",
    GameStatus.entries
)
