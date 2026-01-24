package org.octavius.localization

/**
 * Reprezentuje formy pluralne dla danego klucza tłumaczenia.
 *
 * @property one Forma dla liczby 1 (np. "element")
 * @property few Forma dla 2-4 (np. "elementy")
 * @property many Forma dla pozostałych (np. "elementów")
 */
data class PluralForms(
    val one: String?,
    val few: String?,
    val many: String
)

/**
 * Interfejs dla danych tłumaczeń konkretnego języka.
 *
 * Implementacje tego interfejsu są generowane automatycznie
 * przez task `generateTranslationAccessors` na podstawie plików JSON.
 *
 * Przykład użycia:
 * ```kotlin
 * object TranslationsPl : TranslationData {
 *     override val simple = mapOf("action.save" to "Zapisz")
 *     override val plural = mapOf("items" to PluralForms("element", "elementy", "elementów"))
 * }
 * ```
 */
interface TranslationData {
    /** Proste tłumaczenia: klucz -> tekst */
    val simple: Map<String, String>

    /** Tłumaczenia z formami pluralnymi: klucz -> formy */
    val plural: Map<String, PluralForms>
}
