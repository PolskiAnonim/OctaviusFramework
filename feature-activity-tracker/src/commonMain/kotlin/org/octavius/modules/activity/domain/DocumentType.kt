package org.octavius.modules.activity.domain

import org.octavius.data.annotation.PgEnum
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

@PgEnum
enum class DocumentType : EnumWithFormatter<DocumentType> {
    Pdf,
    Word,
    Excel,
    Powerpoint,
    Text,
    Code,
    Image,
    Video,
    Url,
    Other;

    override fun toDisplayString(): String {
        return when (this) {
            Pdf -> Tr.ActivityTracker.DocumentType.pdf()
            Word -> Tr.ActivityTracker.DocumentType.word()
            Excel -> Tr.ActivityTracker.DocumentType.excel()
            Powerpoint -> Tr.ActivityTracker.DocumentType.powerpoint()
            Text -> Tr.ActivityTracker.DocumentType.text()
            Code -> Tr.ActivityTracker.DocumentType.code()
            Image -> Tr.ActivityTracker.DocumentType.image()
            Video -> Tr.ActivityTracker.DocumentType.video()
            Url -> "TMP"
            Other -> Tr.ActivityTracker.DocumentType.other()
        }
    }
}
