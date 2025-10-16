package org.octavius.database.builder

import org.octavius.data.builder.StepBuilderMethods
import org.octavius.data.transaction.TransactionStep
import kotlin.reflect.KClass

/**
 * Wrapper który zapewnia te same metody terminalne co AbstractQueryBuilder,
 * ale zamiast wykonywać zapytania tworzy TransactionStep dla BatchExecutor.
 */
@Suppress("UNCHECKED_CAST")
internal class StepBuilder<R : AbstractQueryBuilder<R>>(private val builder: R): StepBuilderMethods {

    /** Tworzy TransactionStep z metodą toList */
    override fun toList(params: Map<String, Any?>): TransactionStep<List<Map<String, Any?>>> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).toList(p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingle */
    override fun toSingle(params: Map<String, Any?>): TransactionStep<Map<String, Any?>?> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).toSingle(p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toListOf */
    override fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<List<T>> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).toListOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingleOf */
    override fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<T?> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).toSingleOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toField */
    override fun <T: Any> toField(params: Map<String, Any?>): TransactionStep<T?> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).toField(p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toColumn */
    override fun <T: Any> toColumn(params: Map<String, Any?>): TransactionStep<List<T?>> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).toColumn(p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą execute */
    override fun execute(params: Map<String, Any?>): TransactionStep<Int> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as R).execute(p) },
            params = params
        )
    }
}