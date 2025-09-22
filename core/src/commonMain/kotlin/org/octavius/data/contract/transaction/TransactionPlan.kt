package org.octavius.data.contract.transaction

import org.octavius.data.contract.DataAccess

class TransactionPlan(private val dataAccess: DataAccess) {

    private val steps = mutableListOf<TransactionStep<*>>()

    /**
     * Dodaje prosty krok INSERT do planu.
     * @return Referencja do tego kroku, aby można było użyć jego wyników w kolejnych krokach.
     */
    fun insert(
        tableName: String,
        data: Map<String, Any?>,
        returning: List<String> = emptyList()
    ): StepReference {
        // Używamy prawdziwego buildera pod spodem
        val builder = dataAccess.insertInto(tableName, data.keys.toList())
            .values(data)

        if (returning.isNotEmpty()) {
            builder.returning(returning.joinToString(", "))
        }

        // Tworzymy krok, który zostanie wykonany później
        val step = if (returning.isNotEmpty()) {
            // Jeśli jest RETURNING, oczekujemy wyniku (nawet jeśli to tylko lista map)
            builder.asStep().toList(data)
        } else {
            // Jeśli nie ma, wykonujemy execute i oczekujemy liczby zmienionych wierszy
            builder.asStep().execute(data)
        }

        steps.add(step)
        return StepReference(steps.lastIndex)
    }

    /**
     * Dodaje krok UPDATE do planu.
     */
    fun update(
        tableName: String,
        data: Map<String, Any?>,
        filter: Map<String, Any?>,
        returning: List<String> = emptyList()
    ): StepReference {
        val builder = dataAccess.update(tableName)
            .setValues(data)
            .where(filter.keys.joinToString(" AND ") { "$it = :$it" })

        if (returning.isNotEmpty()) {
            builder.returning(returning.joinToString(", "))
        }

        val allParams = data + filter
        val step = if (returning.isNotEmpty()) {
            builder.asStep().toList(allParams)
        } else {
            builder.asStep().execute(allParams)
        }

        steps.add(step)
        return StepReference(steps.lastIndex)
    }

    /**
     * Dodaje krok DELETE do planu.
     */
    fun delete(
        tableName: String,
        filter: Map<String, Any?>,
        returning: List<String> = emptyList()
    ): StepReference {
        val builder = dataAccess.deleteFrom(tableName)
            .where(filter.keys.joinToString(" AND ") { "$it = :$it" })

        if (returning.isNotEmpty()) {
            builder.returning(returning.joinToString(", "))
        }
        val step = if (returning.isNotEmpty()) {
            builder.asStep().toList(filter)
        } else {
            builder.asStep().execute(filter)
        }
        steps.add(step)
        return StepReference(steps.lastIndex)
    }

    /**
     * Umożliwia dodanie kroku ze złożonego, ręcznie skonfigurowanego buildera.
     */
    fun add(step: TransactionStep<*>): StepReference {
        steps.add(step)
        return StepReference(steps.lastIndex)
    }

    /**
     * Zwraca finalną, gotową do wykonania listę kroków.
     */
    fun build(): List<TransactionStep<*>> {
        return steps.toList() // Zwracamy niemutowalną kopię
    }
}