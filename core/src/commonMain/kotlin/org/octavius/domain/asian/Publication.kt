package org.octavius.domain.asian

import org.octavius.data.annotation.PgType
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T

@PgType
enum class PublicationStatus : EnumWithFormatter<PublicationStatus> {
    NotReading,
    Reading,
    Completed,
    PlanToRead;

    override fun toDisplayString(): String {
        return when (this) {
            NotReading -> T.get("asianMedia.readingStatus.notReading")
            Reading -> T.get("asianMedia.readingStatus.reading")
            Completed -> T.get("asianMedia.readingStatus.completed")
            PlanToRead -> T.get("asianMedia.readingStatus.toRead")
        }
    }
}

@PgType
enum class PublicationLanguage : EnumWithFormatter<PublicationLanguage> {
    Korean,
    Chinese,
    Japanese;

    override fun toDisplayString(): String {
        return when (this) {
            Korean -> T.get("asianMedia.publicationLanguage.korean")
            Chinese -> T.get("asianMedia.publicationLanguage.chinese")
            Japanese -> T.get("asianMedia.publicationLanguage.japanese")
        }
    }
}

@PgType
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
            Manga -> T.get("asianMedia.publicationType.manga")
            LightNovel -> T.get("asianMedia.publicationType.lightNovel")
            WebNovel -> T.get("asianMedia.publicationType.webNovel")
            PublishedNovel -> T.get("asianMedia.publicationType.publishedNovel")
            Webtoon -> T.get("asianMedia.publicationType.webtoon")
            Manhwa -> T.get("asianMedia.publicationType.manhwa")
            Manhua -> T.get("asianMedia.publicationType.manhua")
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

    // Czy publikacja używa rozdziałów
    fun hasChapters(): Boolean {
        return true // Wszystkie typy mają jakąś formę podziału na części
    }
}
