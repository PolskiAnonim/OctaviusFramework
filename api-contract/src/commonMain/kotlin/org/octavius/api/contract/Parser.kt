package org.octavius.api.contract

/**
 * Interfejs dla parsera specyficznego dla danej strony.
 */
interface Parser {
    /**
     * Sprawdza, czy ten parser jest odpowiedni dla danego URL.
     * @param url Aktualny adres URL strony.
     * @return `true` jeśli parser może obsłużyć tę stronę.
     */
    fun canParse(url: String): Boolean

    /**
     * Parsuje stronę i zwraca ustrukturyzowane dane.
     * @return Obiekt implementujący `ParsedData` lub `null`, jeśli parsowanie się nie powiodło.
     */
    suspend fun parse(): ParsedData?
}