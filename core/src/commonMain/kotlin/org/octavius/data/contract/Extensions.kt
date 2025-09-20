package org.octavius.data.contract

import org.octavius.data.contract.transaction.DatabaseValue

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

/**
 * Opakowuje wartość w PgTyped w bardziej płynny sposób.
 * Przykład: "my_value".withPgType("text[]")
 */
fun Any?.withPgType(pgType: String): PgTyped = PgTyped(this, pgType)
