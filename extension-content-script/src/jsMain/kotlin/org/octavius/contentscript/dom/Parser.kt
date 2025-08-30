package org.octavius.contentscript.dom

import kotlinx.browser.document
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationType
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.asList

/**
 * Prosty parser do wyciągania danych ze strony MangaUpdates.
 * Używa selektorów CSS do znalezienia odpowiednich elementów.
 */
object MangaUpdatesParser {

    data class PageData(
        val titles: List<String>,
        val type: PublicationType,
        val language: PublicationLanguage
    )

    fun parsePage(): PageData {
        // Główny tytuł
        val mainTitle = document.querySelector(".releasestitle.tabletitle")?.textContent?.trim() ?: ""
        println(mainTitle)
        // Tytuły alternatywne
        val associatedNamesDiv = document.querySelector("[data-cy='info-box-associated']")
        val associatedTitles = associatedNamesDiv?.querySelectorAll("div")
            ?.asList()
            ?.mapNotNull { (it as? HTMLElement)?.innerText?.trim() }
            ?: emptyList()

        // Łączymy i filtrujemy tytuły
        val allTitles = (listOf(mainTitle) + associatedTitles)
            .map { it.replace("\"", "") } // Usuń cudzysłowy
            .filter { it.isNotBlank() && it.isPrimarilyLatinScript() }
            .distinct()

        // Typ publikacji
        val typeDiv = document.querySelector("[data-cy='info-box-type']") as? HTMLDivElement
        val typeString = typeDiv?.innerText?.trim()?.uppercase()
        val detectedType = when (typeString) {
            "MANHWA" -> PublicationType.Manhwa
            "MANHUA" -> PublicationType.Manhua
            "MANGA" -> PublicationType.Manga
            // NovelUpdates będzie miał inne typy
            else -> PublicationType.Manga // Bezpieczny domyślny
        }

        // Wykrywanie języka na podstawie typu
        val language = when (detectedType) {
            PublicationType.Manhwa -> PublicationLanguage.Korean
            PublicationType.Manhua -> PublicationLanguage.Chinese
            else -> PublicationLanguage.Japanese
        }

        return PageData(allTitles, detectedType, language)
    }

    /**
     * Ulepszona funkcja filtrująca. Zwraca `true`, jeśli ciąg znaków
     * składa się głównie ze znaków łacińskich, cyfr i standardowej interpunkcji.
     * Wyklucza większość popularnych niełacińskich skryptów.
     */
    private fun String.isPrimarilyLatinScript(): Boolean {
        // Jeśli string jest pusty lub zawiera tylko spacje, odrzucamy go
        if (this.isBlank()) return false

        // Zwróć prawdę, jeśli w stringu NIE MA ŻADNEGO znaku z poniższych zakresów
        return this.none { char ->
            when (char.code) {
                // Azja Wschodnia
                in 0x3040..0x309F -> true // Hiragana
                in 0x30A0..0x30FF -> true // Katakana
                in 0x4E00..0x9FBF -> true // Kanji / Hanzi (chińskie, japońskie)
                in 0xAC00..0xD7AF -> true // Hangul (koreańskie)

                // Inne skrypty z Twoich przykładów
                in 0x0400..0x04FF -> true // Cyrylica (rosyjski, etc.)
                in 0x0E00..0x0E7F -> true // Tajski
                in 0x0530..0x058F -> true // Ormiański

                // Można dodać więcej w przyszłości, np. arabski, grecki itp.
                // in 0x0600..0x06FF -> true // Arabski
                // in 0x0370..0x03FF -> true // Grecki

                else -> false
            }
        }
    }
}