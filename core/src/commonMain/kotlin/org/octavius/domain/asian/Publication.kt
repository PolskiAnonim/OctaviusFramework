package org.octavius.domain.asian

import org.octavius.data.contract.PgType
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Translations

@PgType
enum class PublicationStatus : EnumWithFormatter<PublicationStatus> {
    NotReading,
    Reading,
    Completed,
    PlanToRead;

    override fun toDisplayString(): String {
        return when (this) {
            NotReading -> Translations.get("asianMedia.readingStatus.notReading")
            Reading -> Translations.get("asianMedia.readingStatus.reading")
            Completed -> Translations.get("asianMedia.readingStatus.completed")
            PlanToRead -> Translations.get("asianMedia.readingStatus.toRead")
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
            Korean -> Translations.get("asianMedia.publicationLanguage.korean")
            Chinese -> Translations.get("asianMedia.publicationLanguage.chinese")
            Japanese -> Translations.get("asianMedia.publicationLanguage.japanese")
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
            Manga -> Translations.get("asianMedia.publicationType.manga")
            LightNovel -> Translations.get("asianMedia.publicationType.lightNovel")
            WebNovel -> Translations.get("asianMedia.publicationType.webNovel")
            PublishedNovel -> Translations.get("asianMedia.publicationType.publishedNovel")
            Webtoon -> Translations.get("asianMedia.publicationType.webtoon")
            Manhwa -> Translations.get("asianMedia.publicationType.manhwa")
            Manhua -> Translations.get("asianMedia.publicationType.manhua")
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
