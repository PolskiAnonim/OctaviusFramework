package org.octavius.novels.form.control.type.selection.dropdown

/**
 * Reprezentuje pojedynczą opcję w liście rozwijanej (dropdown).
 *
 * Enkapsuluje wartość rzeczywistą wraz z tekstem wyświetlanym użytkownikowi.
 * Pozwala na oddzielenie logiki biznesowej (wartość) od prezentacji (tekst).
 *
 * @param T typ wartości przechowywanych w opcji
 * @property value rzeczywista wartość opcji używana w logice aplikacji
 * @property displayText tekst wyświetlany użytkownikowi w interfejsie
 */
data class DropdownOption<T>(
    val value: T,
    val displayText: String
)