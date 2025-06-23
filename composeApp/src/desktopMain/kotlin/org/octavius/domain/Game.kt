package org.octavius.domain

import org.octavius.localization.Translations

enum class GameStatus : EnumWithFormatter<GameStatus> {
    NotPlaying,
    WithoutTheEnd,
    Played,
    ToPlay,
    Playing, ;

    override fun toDisplayString(): String {
        return when (this) {
            NotPlaying -> Translations.get("games.status.notPlaying")
            WithoutTheEnd -> Translations.get("games.status.endless")
            Played -> Translations.get("games.status.played")
            ToPlay -> Translations.get("games.status.toPlay")
            Playing -> Translations.get("games.status.playing")
        }
    }
}