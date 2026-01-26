package org.octavius.modules.asian.parser

import kotlinx.browser.document
import kotlinx.serialization.json.Json
import org.octavius.api.contract.ParsedData
import org.octavius.api.contract.Parser
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationType
import org.octavius.modules.asian.AsianMediaExtensionModule
import org.octavius.modules.asian.model.AsianPublicationData
import org.w3c.dom.HTMLElement

/**
 * Parser do wyciągania danych ze strony NovelUpdates.
 * Używa selektorów CSS do znalezienia odpowiednich elementów.
 */
object NovelUpdatesParser : Parser<AsianPublicationData> {

    override val moduleId: String = AsianMediaExtensionModule.id

    override fun canParse(url: String): Boolean {
        return "novelupdates.com/series/" in url
    }

    override fun serialize(data: AsianPublicationData): String {
        return Json.encodeToString(AsianPublicationData.serializer(), data)
    }

    override suspend fun parse(): ParsedData {
        // Główny tytuł jest w divie z klasą .seriestitlenu
        val mainTitle = document.querySelector(".seriestitlenu")?.textContent?.trim() ?: ""

        // Tytuły alternatywne znajdują się w divie o id #editassociated.
        // Są oddzielone tagami <br>, więc pobieramy innerHTML i dzielimy go.
        val associatedNamesDiv = document.querySelector("#editassociated") as? HTMLElement
        val associatedTitles = associatedNamesDiv?.innerHTML
            ?.split("<br>")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() } ?: emptyList()

        // Łączymy i filtrujemy tytuły, aby zostawić tylko te z alfabetem łacińskim
        val allTitles = (listOf(mainTitle) + associatedTitles)
            .filter { it.isPrimarilyLatinScript() }
            .distinct()

        // Typ publikacji (Web Novel, Light Novel etc.)
        val typeString = document.querySelector("#showtype a")?.textContent?.trim()?.uppercase()
        val detectedType = when (typeString) {
            "WEB NOVEL" -> PublicationType.WebNovel
            "LIGHT NOVEL" -> PublicationType.LightNovel
            "PUBLISHED NOVEL" -> PublicationType.PublishedNovel
            // Bezpieczny domyślny typ dla tej strony
            else -> PublicationType.WebNovel
        }

        // Język oryginału, np. (CN)
        val languageString = document.querySelector("#showtype span")?.textContent
            ?.trim()
            ?.replace(Regex("[()]"), "") // Usuwamy nawiasy

        val language = when (languageString) {
            "CN" -> PublicationLanguage.Chinese
            "KR" -> PublicationLanguage.Korean
            "JP" -> PublicationLanguage.Japanese
            // Domyślnie chiński, ponieważ jest najczęstszy na NovelUpdates
            else -> PublicationLanguage.Chinese
        }

        return AsianPublicationData(
            source = "NovelUpdates",
            titles = allTitles,
            type = detectedType,
            language = language
        )
    }

    /**
     * Zwraca `true`, jeśli ciąg znaków składa się głównie ze znaków łacińskich,
     * cyfr i standardowej interpunkcji. Wyklucza większość popularnych
     * niełacińskich skryptów.
     */
    private fun String.isPrimarilyLatinScript(): Boolean {
        if (this.isBlank()) return false
        return this.none { char ->
            when (char.code) {
                // Azja Wschodnia
                in 0x3040..0x309F -> true // Hiragana
                in 0x30A0..0x30FF -> true // Katakana
                in 0x4E00..0x9FBF -> true // Kanji / Hanzi (chińskie, japońskie)
                in 0xAC00..0xD7AF -> true // Hangul (koreańskie)
                // Inne
                in 0x0400..0x04FF -> true // Cyrylica
                in 0x0E00..0x0E7F -> true // Tajski
                in 0x0530..0x058F -> true // Ormiański
                else -> false
            }
        }
    }
}