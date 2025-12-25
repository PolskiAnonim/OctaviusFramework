package org.octavius.domain.asian

import org.octavius.data.annotation.PgEnum
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T

@PgEnum
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

@PgEnum
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

@PgEnum
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

}
