package org.octavius.data.transaction

import org.octavius.data.DataResult
import org.octavius.data.builder.QueryBuilder

class TransactionStep<T>(
    // All fields must be public so that Executor has access to them
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

