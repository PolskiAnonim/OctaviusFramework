package org.octavius.database.builder

import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.TransactionStep
import org.octavius.data.contract.builder.StepBuilderMethods
import kotlin.reflect.KClass

/**
 * Wrapper który zapewnia te same metody terminalne co AbstractQueryBuilder,
 * ale zamiast wykonywać zapytania tworzy ExtendedDatabaseStep dla BatchExecutor.
 */
internal class StepBuilder<T : AbstractQueryBuilder<T>>(private val builder: T): StepBuilderMethods {

    /** Tworzy ExtendedDatabaseStep z metodą toList */
    override fun toList(params: Map<String, Any?>): TransactionStep.FromBuilder<List<Map<String, Any?>>> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = builder::toList,
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą toSingle */
    override fun toSingle(params: Map<String, Any?>): TransactionStep.FromBuilder<Map<String, Any?>?> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = builder::toSingle,
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą toListOf */
    override fun <R : Any> toListOf(kClass: KClass<R>, params: Map<String, Any?>): TransactionStep.FromBuilder<List<R>> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = { p -> builder.toListOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą toSingleOf */
    override fun <R : Any> toSingleOf(kClass: KClass<R>, params: Map<String, Any?>): TransactionStep.FromBuilder<R?> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = { p -> builder.toSingleOf(kClass, p) },
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą toField */
    override fun <R> toField(params: Map<String, Any?>): TransactionStep.FromBuilder<R?> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = builder::toField,
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą toColumn */
    override fun <R> toColumn(params: Map<String, Any?>): TransactionStep.FromBuilder<List<R?>> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = builder::toColumn,
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą toSingleWithColumnInfo */
    override fun toSingleWithColumnInfo(params: Map<String, Any?>): TransactionStep.FromBuilder<Map<ColumnInfo, Any?>?> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = builder::toSingleWithColumnInfo,
            params = params
        )
    }

    /** Tworzy ExtendedDatabaseStep z metodą execute */
    override fun execute(params: Map<String, Any?>): TransactionStep.FromBuilder<Int> {
        return TransactionStep.FromBuilder(
            builderState = builder,
            terminalMethod = builder::execute,
            params = params
        )
    }
}