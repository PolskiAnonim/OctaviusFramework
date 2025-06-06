package org.octavius.novels.domain

enum class GameStatus : EnumWithFormatter<GameStatus> {
    NotPlaying,
    WithoutTheEnd,
    Played,
    ToPlay,
    Playing, ;

    override fun toDisplayString(): String {
        return when (this) {
            NotPlaying -> "Nie gram"
            WithoutTheEnd -> "Gra bez końca"
            Played -> "Grałem"
            ToPlay -> "Do zagrania"
            Playing -> "Gram"
        }
    }
}