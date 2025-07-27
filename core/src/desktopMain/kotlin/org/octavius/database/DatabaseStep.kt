package org.octavius.database

/**
 * Abstrakcyjna reprezentacja wartości w operacjach bazodanowych.
 *
 * Sealed class umożliwiająca przekazywanie różnych typów wartości w operacjach DatabaseStep:
 * - **Bezpośrednie wartości**: Znane z góry wartości stałe
 * - **Referencje do wyników**: Wartości pochodzące z poprzednich operacji w tej samej transakcji
 *
 * Ta abstrakcja umożliwia tworzenie złożonych transakcji z zależnościami między krokami.
 *
 * @see DatabaseStep
 * @see DatabaseTransactionManager
 */
sealed class DatabaseValue {
    /**
     * Stała wartość znana w momencie definiowania operacji.
     *
     * @param value Wartość do przekazania w operacji (może być null)
     */
    data class Value(val value: Any?) : DatabaseValue()

    /**
     * Referencja do wyniku poprzedniej operacji w tej samej transakcji.
     *
     * Umożliwia używanie wyników wcześniejszych kroków (np. wygenerowane ID)
     * w kolejnych operacjach, tworząc łańcuch zależności.
     *
     * @param stepIndex Indeks operacji na liście (0-based), której wynik chcemy użyć
     * @param resultKey Klucz w mapie wyników tej operacji (np. "id", "slug", "uuid")
     *
     * Przykład:
     * ```kotlin
     * DatabaseValue.FromStep(0, "id") // Użyj ID z pierwszej operacji
     * ```
     */
    data class FromStep(val stepIndex: Int, val resultKey: String) : DatabaseValue()
}

/**
 * Abstrakcyjna reprezentacja operacji bazodanowej w transakcji.
 *
 * Sealed class definiująca wszystkie obsługiwane typy operacji bazodanowych:
 * - **Insert**: Wstawianie nowych rekordów
 * - **Update**: Aktualizacja istniejących rekordów
 * - **Delete**: Usuwanie rekordów
 * - **RawSql**: Wykonywanie dowolnych zapytań SQL
 *
 * Każda operacja może opcjonalnie zwrócić wartości poprzez klauzulę RETURNING,
 * które mogą być użyte w kolejnych krokach transakcji.
 *
 * @see DatabaseValue
 * @see DatabaseTransactionManager.execute
 */
sealed class DatabaseStep {
    /**
     * Lista kolumn do zwrócenia po wykonaniu operacji (klauzula RETURNING).
     *
     * Opcjonalna lista nazw kolumn, które mają być zwrócone po wykonaniu operacji.
     * Wyniki są dostępne dla kolejnych kroków przez DatabaseValue.FromStep.
     * - Pusta lista: brak RETURNING
     * - ["*"]: wszystkie kolumny
     * - ["id", "uuid"]: konkretne kolumny
     */
    abstract val returning: List<String>

    /**
     * Operacja INSERT - wstawienie nowego rekordu.
     *
     * @param tableName Nazwa tabeli docelowej
     * @param data Mapa kolumn do wartości (nazwa kolumny → DatabaseValue)
     * @param returning Lista kolumn do zwrócenia (domyślnie ["id"])
     */
    data class Insert(
        val tableName: String,
        val data: Map<String, DatabaseValue>,
        override val returning: List<String> = listOf("id")
    ) : DatabaseStep()

    /**
     * Operacja UPDATE - aktualizacja istniejących rekordów.
     *
     * @param tableName Nazwa tabeli docelowej
     * @param data Mapa kolumn do nowych wartości
     * @param filter Warunki WHERE (nazwa kolumny → DatabaseValue)
     * @param returning Lista kolumn do zwrócenia (domyślnie pusta)
     */
    data class Update(
        val tableName: String,
        val data: Map<String, DatabaseValue>,
        val filter: Map<String, DatabaseValue>,
        override val returning: List<String> = emptyList()
    ) : DatabaseStep()

    /**
     * Operacja DELETE - usunięcie rekordów.
     *
     * @param tableName Nazwa tabeli docelowej
     * @param filter Warunki WHERE określające rekordy do usunięcia
     * @param returning Lista kolumn do zwrócenia (domyślnie pusta)
     */
    data class Delete(
        val tableName: String,
        val filter: Map<String, DatabaseValue>,
        override val returning: List<String> = emptyList()
    ) : DatabaseStep()

    /**
     * Operacja RawSql - wykonanie dowolnego zapytania SQL.
     *
     * Umożliwia wykonywanie złożonych zapytań, które nie mieszczą się
     * w standardowych operacjach INSERT/UPDATE/DELETE.
     *
     * @param sql Zapytanie SQL z named parameters (format :param)
     * @param params Mapa parametrów do podstawienia
     * @param returning Lista kolumn do zwrócenia (domyślnie pusta)
     */
    data class RawSql(
        val sql: String,
        val params: Map<String, DatabaseValue>,
        override val returning: List<String> = emptyList()
    ) : DatabaseStep()
}