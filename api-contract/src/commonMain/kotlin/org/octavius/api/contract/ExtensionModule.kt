package org.octavius.api.contract

import org.octavius.navigation.Screen
import org.octavius.navigation.Tab

/**
 * Definiuje kontrakt dla modułu wtyczki
 */
interface ExtensionModule {

    /** Unikalne ID modułu, np. "asian-media" */
    val id: String

    /**
     * Deserializuje string JSON do konkretnego obiektu ParsedData,
     * który jest obsługiwany przez ten moduł.
     *
     * @param jsonString Surowy JSON z content scriptu.
     * @return Zdeserializowany obiekt ParsedData lub null, jeśli dane nie są w oczekiwanym formacie.
     */
    fun deserializeData(jsonString: String): ParsedData?

    /**
     * Główna metoda "fabryczna". Na podstawie konkretnego typu danych
     * decyduje, który ekran stworzyć.
     * @param data Dane sparsowane przez content script.
     * @return Obiekt Screen gotowy do wyświetlenia, lub null jeśli dane nie pasują.
     */
    fun createScreenFromData(data: ParsedData): Screen?

    /**
     * Parsery stron (content-script)
     */
    val parsers: List<Parser>
}