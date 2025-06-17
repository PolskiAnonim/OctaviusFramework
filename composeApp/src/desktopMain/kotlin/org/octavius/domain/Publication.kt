package org.octavius.domain

enum class PublicationStatus : EnumWithFormatter<PublicationStatus> {
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

enum class PublicationLanguage : EnumWithFormatter<PublicationLanguage> {
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

enum class PublicationType : EnumWithFormatter<PublicationType> {
    Manga,
    LightNovel,
    WebNovel,
    PublishedNovel,
    Webtoon,
    Manhwa,
    Manhua;

    override fun toDisplayString(): String {
        return when (this) {
            Manga -> "Manga"
            LightNovel -> "Light Novel"
            WebNovel -> "Web Novel"
            PublishedNovel -> "Published Novel"
            Webtoon -> "Webtoon"
            Manhwa -> "Manhwa"
            Manhua -> "Manhua"
        }
    }

    // Dynamiczne etykiety dla tomów
    fun getVolumesLabel(): String {
        return when (this) {
            Manga, LightNovel, PublishedNovel, Manhwa, Manhua -> "Tomy"
            WebNovel -> "Części"
            Webtoon -> "Sezony"
        }
    }

    // Dynamiczne etykiety dla rozdziałów
    fun getChaptersLabel(): String {
        return "Rozdziały"
    }

    // Czy publikacja używa tomów
    fun hasVolumes(): Boolean {
        return this != WebNovel // Web Novel może nie mieć koncepcji tomów
    }

    // Czy publikacja używa rozdziałów
    fun hasChapters(): Boolean {
        return true // Wszystkie typy mają jakąś formę podziału na części
    }
}
