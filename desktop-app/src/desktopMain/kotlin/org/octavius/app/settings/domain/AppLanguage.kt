package org.octavius.app.settings.domain

import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

/**
 * Supported application languages.
 */
enum class AppLanguage(val code: String) : EnumWithFormatter<AppLanguage> {
    PL("pl"),
    EN("en");

    override fun toDisplayString(): String = when (this) {
        PL -> Tr.Settings.Language.pl()
        EN -> Tr.Settings.Language.en()
    }

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find { it.code == code } ?: EN
    }
}
