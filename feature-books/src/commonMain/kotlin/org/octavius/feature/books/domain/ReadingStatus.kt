package org.octavius.feature.books.domain

import io.github.octaviusframework.db.api.annotation.PgEnum
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

@PgEnum(name = "reading_status")
enum class ReadingStatus : EnumWithFormatter<ReadingStatus> {
    NotReading,
    Reading,
    Completed,
    PlanToRead;

    override fun toDisplayString(): String {
        return when (this) {
            NotReading -> Tr.Books.Status.notReading()
            Reading -> Tr.Books.Status.reading()
            Completed -> Tr.Books.Status.completed()
            PlanToRead -> Tr.Books.Status.planToRead()
        }
    }
}