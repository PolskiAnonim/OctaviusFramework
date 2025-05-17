package org.octavius.novels.domain

enum class NovelStatus: EnumWithFormatter<NovelStatus> {
    NotReading,
    Reading,
    Completed,
    PlanToRead;

    override fun toDisplayString(): String {
        return when (this) {
            NotReading -> "Nie czytam"
            Reading -> "Czytam"
            Completed -> "Ukończone"
            PlanToRead -> "Do przeczytania"
        }
    }
}

enum class NovelLanguage: EnumWithFormatter<NovelLanguage> {
    Korean,
    Chinese,
    Japanese;

    override fun toDisplayString(): String {
        return when (this) {
            Korean -> "Koreański"
            Chinese -> "Chiński"
            Japanese -> "Japoński"
        }
    }
}

