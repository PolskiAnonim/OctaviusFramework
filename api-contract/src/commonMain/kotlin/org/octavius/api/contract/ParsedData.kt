package org.octavius.api.contract

/** Ogólny interfejs dla danych sparsowanych ze strony */
interface ParsedData {
    /**
     * Id modułu
     */
    val moduleId: String;

    /**
     * Strona źródłowa
     */
    val source: String;
}