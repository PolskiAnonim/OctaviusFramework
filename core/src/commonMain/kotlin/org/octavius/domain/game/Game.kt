package org.octavius.domain.game

import org.octavius.data.contract.PgType
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T

@PgType
enum class GameStatus : EnumWithFormatter<GameStatus> {
    NotPlaying,
    WithoutTheEnd,
    Played,
    ToPlay,
    Playing, ;

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