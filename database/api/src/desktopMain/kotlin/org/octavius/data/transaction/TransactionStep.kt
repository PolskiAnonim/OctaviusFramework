package org.octavius.data.transaction

import org.octavius.data.DataResult
import org.octavius.data.builder.QueryBuilder

class TransactionStep<T>(
    // Wszystkie pola muszą być publiczne, aby Executor miał do nich dostęp
    val builder: QueryBuilder<*>,
    val executionLogic: (builder: QueryBuilder<*>, params: Map<String, Any?>) -> DataResult<T>,
    val params: Map<String, Any?>
) {
    override fun toString(): String {
        return """
            TransactionStep{
                builder: 
                $builder
                ------------------------------
                params: 
                $params
            }
        """.trimIndent()

    }
}

