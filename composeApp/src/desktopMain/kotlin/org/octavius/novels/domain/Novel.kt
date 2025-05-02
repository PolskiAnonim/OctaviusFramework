package org.octavius.novels.domain

data class Novel(
    val id: Int,
    val titles: List<String>,
    val novelType: String,
    val originalLanguage: NovelLanguage,
    val status: NovelStatus
)

enum class NovelStatus {
    NotReading,
    Reading,
    Completed,
    PlanToRead;

    fun toDisplayString(): String {
        return when (this) {
            NotReading -> "Nie czytam"
            Reading -> "Czytam"
            Completed -> "Ukończone"
            PlanToRead -> "Do przeczytania"
        }
    }
}

enum class NovelLanguage {
    Korean,
    Chinese,
    Japanese;

    fun toDisplayString(): String {
        return when (this) {
            Korean -> "Koreański"
            Chinese -> "Chiński"
            Japanese -> "Japoński"
        }
    }
}

