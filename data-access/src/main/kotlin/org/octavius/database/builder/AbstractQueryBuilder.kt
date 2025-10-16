package org.octavius.database.builder

import io.github.oshai.kotlinlogging.KotlinLogging
import org.octavius.data.DataResult
import org.octavius.data.builder.QueryBuilder
import org.octavius.data.builder.StepBuilderMethods
import org.octavius.data.exception.DatabaseException
import org.octavius.data.exception.QueryExecutionException
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import kotlin.reflect.KClass

/**
 * Klasa bazowa dla wszystkich builderów, które mogą zwracać wyniki w postaci wierszy danych
 * (przez `SELECT` lub klauzulę `RETURNING`).
 *
 * Unifikuje API dla metod terminalnych (`toList`, `toSingle`, `toField` itd.) oraz logikę budowy
 * zapytań z klauzulą WITH (Common Table Expressions). Używa generyków, aby zapewnić płynny
 * interfejs (fluent API) w podklasach.
 */
internal abstract class AbstractQueryBuilder<R : AbstractQueryBuilder<R>>(
    protected val jdbcTemplate: NamedParameterJdbcTemplate,
    private val kotlinToPostgresConverter: KotlinToPostgresConverter,
    protected val rowMappers: RowMappers,
    protected val table: String? = null,
): QueryBuilder {
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
        return returningClause?.let { " RETURNING $it" } ?: ""
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
    fun recursive(recursive: Boolean): R = apply {
        this.recursiveWith = recursive
    } as R

    /**
     * Tworzy fragment SQL dla klauzuli WITH na podstawie dodanych zapytań.
     */
    protected fun buildWithClause(): String {
        if (withClauses.isEmpty()) return ""
        val sb = StringBuilder("WITH ")
        if (recursiveWith) {
            sb.append("RECURSIVE ")
        }
        sb.append(withClauses.joinToString(", ") { "${it.first} AS (${it.second})" })
        sb.append(" ")
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
    fun <T: Any> toField(params: Map<String, Any?>): DataResult<T?> {
        return executeReturningQuery(params, rowMappers.SingleValueMapper()) {
            @Suppress("UNCHECKED_CAST")
            DataResult.Success(it.firstOrNull() as T?)
        }
    }

    /** Wykonuje zapytanie i zwraca listę wartości z pierwszej kolumny wszystkich wierszy. */
    fun <T: Any> toColumn(params: Map<String, Any?>): DataResult<List<T?>> {
        return executeReturningQuery(params, rowMappers.SingleValueMapper()) {
            @Suppress("UNCHECKED_CAST")
            DataResult.Success(it as List<T?>)
        }
    }

    fun forEachRow(params: Map<String, Any?>, action: (row: Map<String, Any?>) -> Unit): DataResult<Unit> {
        // Sprawdzamy te same warunki co dla toList()
        if (!canReturnResultsByDefault && returningClause == null) {
            throw IllegalStateException("Nie można wywołać forEachRow() na zapytaniu modyfikującym bez klauzuli RETURNING.")
        }

        val sql = buildSql()

        return execute(sql, params) { expandedSql, expandedParams ->

            val rowMapper = rowMappers.ColumnNameMapper()

            jdbcTemplate.query(expandedSql, expandedParams, RowCallbackHandler { rs ->
                // Ta lambda jest ciałem metody processRow. Spring wywoła ją dla każdego wiersza.
                // Numer wiersza nie jest potrzebny, więc można przekazać 0.
                val mappedRow = rowMapper.mapRow(rs, 0)

                // Wywołujemy akcję użytkownika na zmapowanym wierszu
                action(mappedRow)
            })

            // Jeśli pętla się zakończyła bez wyjątku, operacja się powiodła.
            DataResult.Success(Unit)
        }
    }

    fun <T : Any> forEachRowOf(kClass: KClass<T>, params: Map<String, Any?>, action: (obj: T) -> Unit): DataResult<Unit> {
        if (!canReturnResultsByDefault && returningClause == null) {
            throw IllegalStateException("Nie można wywołać forEachRowOf() na zapytaniu modyfikującym bez klauzuli RETURNING.")
        }

        val sql = buildSql()

        return execute(sql, params) { expandedSql, expandedParams ->

            val rowMapper = rowMappers.DataObjectMapper(kClass)

            jdbcTemplate.query(expandedSql, expandedParams) { rs ->
                val mappedObject = rowMapper.mapRow(rs, 0)
                action(mappedObject)
            }

            DataResult.Success(Unit)
        }
    }

    /** Zwraca wygenerowany string SQL bez wykonywania zapytania. */
    fun toSql(): String {
        return buildSql()
    }

    /**
     * Wykonuje zapytanie modyfikujące (bez RETURNING) i zwraca liczbę zmienionych wierszy.
     * Rzuca wyjątek, jeśli klauzula RETURNING została użyta - w takim przypadku należy
     * użyć metod `toList()`, `toSingle()` itp.
     */
    fun execute(params: Map<String, Any?>): DataResult<Int> {
        if (returningClause != null) {
            throw IllegalStateException("Użyj metod toList(), toSingle() etc., gdy zdefiniowano klauzulę RETURNING.")
        }
        val sql = buildSql()
        return execute(sql, params) { expandedSql, expandedParams ->
            val affectedRows = jdbcTemplate.update(expandedSql, expandedParams)
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
        if (!canReturnResultsByDefault && returningClause == null) {
            throw IllegalStateException("Nie można wywołać toList(), toSingle() etc. na zapytaniu modyfikującym bez klauzuli RETURNING. Użyj .returning().")
        }
        val sql = buildSql()
        return execute(sql, params) { expandedSql, expandedParams ->
            val results: List<M> = jdbcTemplate.query(expandedSql, expandedParams, rowMapper)
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
        action: (expandedSql: String, expandedParams: Map<String, Any?>) -> DataResult<R>
    ): DataResult<R> {
        var expandedSql: String? = null
        var expandedParams: Map<String, Any?>? = null
        return try {
            val expanded = kotlinToPostgresConverter.expandParametersInQuery(sql, params)
            expandedSql = expanded.expandedSql
            expandedParams = expanded.expandedParams
            logger.debug { "Executing query (expanded): $expandedSql with params: $expandedParams" }
            action(expandedSql, expandedParams)
        } catch (e: DatabaseException) {
            logger.error(e) { "Database error executing query: $expandedSql with params: $expandedParams" }
            DataResult.Failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error executing query: $expandedSql with params: $expandedParams" }
            DataResult.Failure(
                QueryExecutionException(
                    "Unexpected error during query execution.",
                    sql = expandedSql ?: "SQL not generated",
                    params = expandedParams ?: params,
                    cause = e
                )
            )
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                          STEP BUILDER CONVERSION
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Konwertuje ten builder na StepBuilder, który umożliwia lazy execution w ramach transakcji.
     * Zwraca wrapper z metodami terminalnymi, które tworzą ExtendedDatabaseStep zamiast wykonywać zapytanie.
     */
    override fun asStep(): StepBuilderMethods {
        @Suppress("UNCHECKED_CAST")
        return StepBuilder(this as R)
    }
}