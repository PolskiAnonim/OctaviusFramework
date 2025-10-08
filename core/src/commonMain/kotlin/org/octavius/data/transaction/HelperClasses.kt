package org.octavius.data.transaction

import org.octavius.data.DataResult

/**
 * Reprezentuje wartość w kroku bazodanowym.
 *
 * Umożliwia przekazywanie zarówno stałych wartości, jak i dynamicznych referencji
 * do wyników poprzednich kroków w tej samej transakcji.
 *
 * @see TransactionStep
 */
sealed class DatabaseValue {
    /**
     * Stała, predefiniowana wartość.
     * @param value Wartość do użycia w operacji.
     */
    data class Value(val value: Any?) : DatabaseValue()

    /**
     * Referencja do wyniku z poprzedniego kroku w tej samej transakcji.
     *
     * @param stepIndex Indeks (0-based) kroku, którego wynik ma być użyty.
     * @param resultKey Nazwa kolumny (np. "id") w wyniku tego kroku.
     */
    data class FromStep(val stepIndex: Int, val resultKey: String) : DatabaseValue()
}

/**
 * Reprezentuje pojedynczą operację w transakcji bazodanowej.
 */
class TransactionStep<T>(
    val builderState: Any, // Będzie to konkretny builder (DatabaseSelectQueryBuilder, etc.)
    val terminalMethod: (Map<String, Any?>) -> DataResult<T>,
    val params: Map<String, Any?>
)

typealias TransactionPlanResults = Map<Int, List<Map<String, Any?>>>

/**
 * Konwertuje dowolną  wartość (także null) na instancję [DatabaseValue.Value].
 *
 * Stanowi zwięzłą alternatywę dla jawnego wywołania konstruktora,
 * poprawiając czytelność operacji budujących kroki transakcji.
 *
 * Przykład użycia:
 * `val idRef = 123.toDatabaseValue()` zamiast `val idRef = DatabaseValue.Value(123)`
 *
 * @return Instancja [DatabaseValue.Value] opakowująca tę wartość.
 * @see DatabaseValue
 */
fun Any?.toDatabaseValue(): DatabaseValue = DatabaseValue.Value(this)
