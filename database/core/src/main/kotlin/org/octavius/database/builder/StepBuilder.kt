package org.octavius.database.builder

import org.octavius.data.builder.StepBuilderMethods
import org.octavius.data.transaction.TransactionStep
import kotlin.reflect.KClass

/**
 * Wrapper który zapewnia te same metody terminalne co AbstractQueryBuilder,
 * ale zamiast wykonywać zapytania tworzy TransactionStep dla BatchExecutor.
 */
internal class StepBuilder(private val builder: AbstractQueryBuilder<*>) : StepBuilderMethods {

    /** Tworzy TransactionStep z metodą toList */
    override fun toList(params: Map<String, Any?>): TransactionStep<List<Map<String, Any?>>> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).toList(p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingle */
    override fun toSingle(params: Map<String, Any?>): TransactionStep<Map<String, Any?>?> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).toSingle(p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toListOf */
    override fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<List<T>> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).toListOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingleOf */
    override fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<T?> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).toSingleOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toField */
    override fun <T : Any> toField(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<T?> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).toField(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toColumn */
    override fun <T : Any> toColumn(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<List<T?>> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).toColumn(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą execute */
    override fun execute(params: Map<String, Any?>): TransactionStep<Int> {
        return TransactionStep(
            builder = this.builder,
            executionLogic = { b, p -> (b as AbstractQueryBuilder<*>).execute(p) },
            params = params
        )
    }
}