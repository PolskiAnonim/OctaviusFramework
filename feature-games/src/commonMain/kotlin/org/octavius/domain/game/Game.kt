package org.octavius.domain.game

import io.github.octaviusframework.db.api.annotation.PgEnum
import io.github.octaviusframework.db.api.serializer.EnumWithCaseConventionSerializer
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

@PgEnum
enum class GameStatus : EnumWithFormatter<GameStatus> {
    NotPlaying,
    WithoutTheEnd,
    Played,
    ToPlay,
    Playing;

    override fun toDisplayString(): String {
        return when (this) {
            NotPlaying -> Tr.Games.Status.notPlaying()
            WithoutTheEnd -> Tr.Games.Status.endless()
            Played -> Tr.Games.Status.played()
            ToPlay -> Tr.Games.Status.toPlay()
            Playing -> Tr.Games.Status.playing()
        }
    }
}

object GameStatusDynamicDtoSerializer : EnumWithCaseConventionSerializer<GameStatus>(
    "GameStatus",
    GameStatus.entries
)
