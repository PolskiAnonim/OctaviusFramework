package org.octavius.data.transaction

/**
 * Definiuje zachowanie transakcji, gdy jest uruchamiana w kontekście już istniejącej transakcji.
 * Odzwierciedla kluczowe strategie propagacji ze Spring Framework.
 */
enum class TransactionPropagation {
    /**
     * Domyślne zachowanie. Jeśli transakcja już istnieje, dołącz do niej.
     * Jeśli nie, utwórz nową.
     */
    REQUIRED,

    /**
     * Zawsze twórz nową, niezależną transakcję.
     * Istniejąca transakcja zostanie wstrzymana na czas wykonania nowej.
     * Użyteczne np. do zapisu logów audytowych, które muszą się powieść
     * nawet jeśli główna operacja zostanie wycofana.
     */
    REQUIRES_NEW,

    /**
     * Wykonaj w zagnieżdżonej transakcji, jeśli transakcja już istnieje.
     * Używa mechanizmu SAVEPOINT w bazie danych. Pozwala to na niezależne
     * wycofanie zagnieżdżonej transakcji bez wpływu na transakcję zewnętrzną.
     * Jeśli transakcja nadrzędna nie istnieje, działa jak [REQUIRED].
     */
    NESTED
}