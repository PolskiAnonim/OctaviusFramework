package org.octavius.database.builder

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.octavius.data.DataResult
import org.octavius.data.builder.AsyncTerminalMethods
import org.octavius.data.builder.QueryBuilder
import org.octavius.data.builder.StepBuilderMethods
import org.octavius.data.builder.StreamingTerminalMethods
import org.octavius.data.exception.QueryExecutionException
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.octavius.database.type.PositionalQuery
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Klasa bazowa dla wszystkich builderów, które mogą zwracać wyniki w postaci wierszy danych
 * (przez `SELECT` lub klauzulę `RETURNING`).
 *
 * Unifikuje API dla metod terminalnych (`toList`, `toSingle`, `toField` itd.) oraz logikę budowy
 * zapytań z klauzulą WITH (Common Table Expressions). Używa generyków, aby zapewnić płynny
 * interfejs (fluent API) w podklasach.
 */
internal abstract class AbstractQueryBuilder<R : QueryBuilder<R>>(
    val jdbcTemplate: JdbcTemplate,
    val kotlinToPostgresConverter: KotlinToPostgresConverter,
    val rowMappers: RowMappers,
    protected val table: String? = null,
): QueryBuilder<R> {
    companion object {
        private val logger = KotlinLogging.logger {}
    }
    // Bardzo nie chcemy żeby SELECT umierał przy wykonywaniu zapytań
    protected abstract val canReturnResultsByDefault: Boolean
    //------------------------------------------------------------------------------------------------------------------
    //                                 ABSTRAKCYJNA METODA DO IMPLEMENTACJI
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Buduje finalne zapytanie SQL na podstawie stanu buildera.
     * Musi być zaimplementowane przez każdą konkretną klasę buildera.
     */
    abstract fun buildSql(): String

    //------------------------------------------------------------------------------------------------------------------
    //                                         RETURNING CLAUSE (dla INSERT/UPDATE/DELETE)
    //------------------------------------------------------------------------------------------------------------------
    protected var returningClause: String? = null

    /**
     * Dodaje klauzulę RETURNING do zapytania modyfikującego (INSERT, UPDATE, DELETE).
     * @param columns Kolumny do zwrócenia po wykonaniu operacji.
     */
    @Suppress("UNCHECKED_CAST")
    fun returning(vararg columns: String): R = apply {
        this.returningClause = columns.joinToString(", ")
    } as R

    /**
     * Buduje fragment SQL dla klauzuli RETURNING.
     */
    protected fun buildReturningClause(): String {
        return returningClause?.let { "\nRETURNING $it" } ?: ""
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                              CTE (WITH)
    //------------------------------------------------------------------------------------------------------------------
    protected val withClauses: MutableList<Pair<String, String>> = mutableListOf()
    protected var recursiveWith: Boolean = false


    /**
     * Dodaje zapytanie do klauzuli WITH (Common Table Expression).
     * @param name Nazwa (alias) dla CTE.
     * @param query Zapytanie SQL definiujące CTE.
     */
    @Suppress("UNCHECKED_CAST")
    fun with(name: String, query: String): R = apply {
        withClauses.add(name to query)
    } as R

    /**
     * Oznacza klauzulę WITH jako rekurencyjną.
     */
    @Suppress("UNCHECKED_CAST")
    fun recursive(): R = apply {
        this.recursiveWith = true
    } as R

    /**
     * Tworzy sformatowany fragment SQL dla klauzuli WITH na podstawie dodanych zapytań.
     * Każde CTE jest w nowej linii dla czytelności.
     */
    protected fun buildWithClause(): String {
        if (withClauses.isEmpty()) return ""
        val sb = StringBuilder("WITH ")
        if (recursiveWith) {
            sb.append("RECURSIVE ")
        }
        // Każde CTE w nowej linii z wcięciem
        sb.append(withClauses.joinToString(",\n  ") { "${it.first} AS (${it.second})" })
        // Nowa linia oddzielająca WITH od głównego zapytania
        sb.append("\n")
        return sb.toString()
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                            METODY TERMINALNE
    //------------------------------------------------------------------------------------------------------------------

    // --- Mapowanie do Map<String, Any?> ---

    /** Wykonuje zapytanie i zwraca listę wierszy jako `List<Map<String, Any?>>`. */
    fun toList(params: Map<String, Any?>): DataResult<List<Map<String, Any?>>> {
        return executeReturningQuery(params, rowMappers.ColumnNameMapper()) { DataResult.Success(it) }
    }

    /** Wykonuje zapytanie i zwraca pojedynczy wiersz jako `Map<String, Any?>?`. */
    fun toSingle(params: Map<String, Any?>): DataResult<Map<String, Any?>?> {
        return executeReturningQuery(params, rowMappers.ColumnNameMapper()) { DataResult.Success(it.firstOrNull()) }
    }

    // --- Mapowanie do obiektów na podstawie KClass ---

    /** Wykonuje zapytanie i mapuje wyniki na listę obiektów podanej klasy. */
    fun <T : Any> toListOf(kClass: KClass<T>, params: Map<String, Any?>): DataResult<List<T>> {
        return executeReturningQuery(params, rowMappers.DataObjectMapper(kClass)) { DataResult.Success(it) }
    }

    /** Wykonuje zapytanie i mapuje wynik na pojedynczy obiekt podanej klasy. */
    fun <T : Any> toSingleOf(kClass: KClass<T>, params: Map<String, Any?>): DataResult<T?> {
        return executeReturningQuery(
            params,
            rowMappers.DataObjectMapper(kClass)
        ) { DataResult.Success(it.firstOrNull()) }
    }

    // --- Mapowanie do pojedynczych wartości (skalarne) ---

    /** Wykonuje zapytanie i zwraca wartość z pierwszej kolumny pierwszego wiersza. */
    fun <T: Any> toField(targetType: KType, params: Map<String, Any?>): DataResult<T?> {
        return executeReturningQuery(params, rowMappers.SingleValueMapper<T>(targetType)) {
            DataResult.Success(it.firstOrNull())
        }
    }

    /** Wykonuje zapytanie i zwraca listę wartości z pierwszej kolumny wszystkich wierszy. */
    fun <T: Any> toColumn(targetType: KType, params: Map<String, Any?>): DataResult<List<T?>> {
        return executeReturningQuery(params, rowMappers.SingleValueMapper<T>(targetType)) {
            DataResult.Success(it)
        }
    }

    /** Zwraca wygenerowany string SQL bez wykonywania zapytania. */
    fun toSql(): String {
        return buildSql()
    }

    override fun toString(): String {
        return toSql()
    }

    /**
     * Wykonuje zapytanie modyfikujące (bez RETURNING) i zwraca liczbę zmienionych wierszy.
     * Rzuca wyjątek, jeśli klauzula RETURNING została użyta - w takim przypadku należy
     * użyć metod `toList()`, `toSingle()` itp.
     */
    fun execute(params: Map<String, Any?>): DataResult<Int> {
        check(returningClause == null) { "Użyj metod toList(), toSingle() etc., gdy zdefiniowano klauzulę RETURNING." }
        val sql = buildSql()
        return execute(sql, params) { positionalSql, positionalParams ->
            val affectedRows = jdbcTemplate.update(positionalSql, *positionalParams.toTypedArray())
            DataResult.Success(affectedRows)
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                          WYKONYWANIE ZAPYTAŃ
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Prywatna metoda pomocnicza do wykonywania zapytań zwracających wiersze.
     * @param params Parametry zapytania.
     * @param rowMapper Sposób mapowania pojedynczego wiersza.
     * @param transform Funkcja przekształcająca zmapowaną listę wyników w finalny [DataResult].
     */
    private fun <R, M> executeReturningQuery(
        params: Map<String, Any?>,
        rowMapper: RowMapper<M>,
        transform: (List<M>) -> DataResult<R>
    ): DataResult<R> {
        check(canReturnResultsByDefault || returningClause != null) { "Nie można wywołać toList(), toSingle() etc. na zapytaniu modyfikującym bez klauzuli RETURNING. Użyj .returning()." }
        val sql = buildSql()
        return execute(sql, params) { positionalSql, positionalParams ->
            val results: List<M> = jdbcTemplate.query(positionalSql, rowMapper, *positionalParams.toTypedArray())
            transform(results)
        }
    }

    //  ---GŁÓWNA METODA WYKONUJĄCA ZAPYTANIA---

    /**
     * Generyczna funkcja do wykonywania zapytań, opakowująca logikę w obsługę błędów,
     * konwersję typów i logowanie.
     *
     * @param sql Zapytanie SQL do wykonania.
     * @param params Mapa parametrów.
     * @param action Lambda, która zostanie wykonana z przygotowanym zapytaniem i parametrami.
     * @return Wynik operacji jako [DataResult].
     */
    protected fun <R> execute(
        sql: String,
        params: Map<String, Any?>,
        action: (positionalSql: String, positionalParams: List<Any?>) -> DataResult<R>
    ): DataResult<R> {
        var positionalQuery: PositionalQuery? = null
        return try {
            positionalQuery = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
            logger.debug {
                """
                Executing query (original): $sql with params: $params
                  -> (expanded): ${positionalQuery.sql} with positional params: ${positionalQuery.params}
                """.trimIndent()
            }
            action(positionalQuery.sql, positionalQuery.params)
        } catch (e: Exception) {
            val executionException = QueryExecutionException(
                sql = sql,
                params = params,
                expandedSql = positionalQuery?.sql,
                expandedParams = positionalQuery?.params,
                cause = e
            )

            logger.error(executionException) { "Database error occurred" }

            DataResult.Failure(executionException)
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                          BUILDER CONVERSION
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Konwertuje ten builder na StepBuilder, który umożliwia lazy execution w ramach transakcji.
     * Zwraca wrapper z metodami terminalnymi, które tworzą ExtendedDatabaseStep zamiast wykonywać zapytanie.
     */
    override fun asStep(): StepBuilderMethods {
        @Suppress("UNCHECKED_CAST")
        return StepBuilder(this)
    }


    override fun async(scope: CoroutineScope, ioDispatcher: CoroutineDispatcher): AsyncTerminalMethods {
        return AsyncQueryBuilder(this, scope, ioDispatcher)
    }

    override fun asStream(fetchSize: Int): StreamingTerminalMethods {
        // Po prostu tworzymy i zwracamy nową instancję naszego egzekutora
        return StreamingQueryBuilder(this, fetchSize)
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                          KOPIA BUILDERA
    //------------------------------------------------------------------------------------------------------------------


    /**
     * Kopiuje stan z innego buildera tego samego typu.
     * Używane przez metody `copy()` w klasach pochodnych.
     */
    protected fun copyBaseStateFrom(source: AbstractQueryBuilder<R>) {
        this.returningClause = source.returningClause
        this.withClauses.clear()
        this.withClauses.addAll(source.withClauses)
        this.recursiveWith = source.recursiveWith
    }

    abstract override fun copy(): R
}
