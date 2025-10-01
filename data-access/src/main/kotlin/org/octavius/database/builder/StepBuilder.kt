package org.octavius.database.builder

import org.octavius.data.ColumnInfo
import org.octavius.data.builder.StepBuilderMethods
import org.octavius.data.transaction.TransactionStep
import kotlin.reflect.KClass

/**
 * Wrapper który zapewnia te same metody terminalne co AbstractQueryBuilder,
 * ale zamiast wykonywać zapytania tworzy ExtendedDatabaseStep dla BatchExecutor.
 */
internal class StepBuilder<R : AbstractQueryBuilder<R>>(private val builder: R): StepBuilderMethods {

    /** Tworzy TransactionStep z metodą toList */
    override fun toList(params: Map<String, Any?>): TransactionStep<List<Map<String, Any?>>> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = builder::toList,
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingle */
    override fun toSingle(params: Map<String, Any?>): TransactionStep<Map<String, Any?>?> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = builder::toSingle,
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toListOf */
    override fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<List<T>> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = { p -> builder.toListOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingleOf */
    override fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): TransactionStep<T?> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = { p -> builder.toSingleOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toField */
    override fun <T: Any> toField(params: Map<String, Any?>): TransactionStep<T?> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = builder::toField,
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toColumn */
    override fun <T: Any> toColumn(params: Map<String, Any?>): TransactionStep<List<T?>> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = builder::toColumn,
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą toSingleWithColumnInfo */
    override fun toSingleWithColumnInfo(params: Map<String, Any?>): TransactionStep<Map<ColumnInfo, Any?>?> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = builder::toSingleWithColumnInfo,
            params = params
        )
    }

    /** Tworzy TransactionStep z metodą execute */
    override fun execute(params: Map<String, Any?>): TransactionStep<Int> {
        return TransactionStep(
            builderState = builder,
            terminalMethod = builder::execute,
            params = params
        )
    }
}