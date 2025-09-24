package org.octavius.database.builder

import org.octavius.data.contract.builder.SelectQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Wewnętrzna implementacja [SelectQueryBuilder] do budowania zapytań SQL SELECT.
 * Dziedziczy z [AbstractQueryBuilder], aby ponownie wykorzystać logikę
 * klauzuli WITH i metod terminalnych.
 */
internal class DatabaseSelectQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    rowMappers: RowMappers,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    private val selectClause: String
) : AbstractQueryBuilder<DatabaseSelectQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, null), SelectQueryBuilder {

    //------------------------------------------------------------------------------------------------------------------
    //                                    STAN WEWNĘTRZNY KLAUZULI SELECT
    //------------------------------------------------------------------------------------------------------------------

    private var fromClause: String? = null
    private var whereCondition: String? = null
    private var groupByClause: String? = null
    private var havingClause: String? = null
    private var orderByClause: String? = null
    private var limitValue: Long? = null
    private var offsetValue: Long? = null

    //------------------------------------------------------------------------------------------------------------------
    //                                      BUDOWANIE KLAUZULI SELECT
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Ustawia klauzulę FROM.
     * Programista jest w pełni odpowiedzialny za przekazanie poprawnej składni.
     * Metoda nie dokonuje żadnego formatowania ani opakowywania w nawiasy.
     *
     * Przykłady poprawnych wartości:
     * - "users"
     * - "users u"
     * - "users AS u JOIN profiles p ON u.id = p.user_id"
     * - "(SELECT id FROM active_users) AS u"
     * - "UNNEST(:ids) AS id"
     *
     */
    override fun from(source: String): SelectQueryBuilder = apply {
        this.fromClause = source
    }

    override fun fromSubquery(subquery: String, alias: String?): SelectQueryBuilder {
        val query = if (alias == null) {
            "($subquery)"
        } else {
            "($subquery) AS $alias"
        }
        return this.from(query)
    }

    override fun where(condition: String?): SelectQueryBuilder = apply {
        this.whereCondition = condition
    }

    override fun groupBy(columns: String?): SelectQueryBuilder = apply {
        this.groupByClause = columns
    }

    override fun having(condition: String?): SelectQueryBuilder = apply {
        this.havingClause = condition
    }

    override fun orderBy(ordering: String?): SelectQueryBuilder = apply {
        this.orderByClause = ordering
    }

    override fun limit(count: Long?): SelectQueryBuilder = apply {
        this.limitValue = count
    }

    override fun offset(position: Long): SelectQueryBuilder = apply {
        this.offsetValue = position
    }

    override fun page(page: Long, size: Long): SelectQueryBuilder = apply {
        require(page >= 0) { "Page number cannot be negative." }
        require(size > 0) { "Page size must be positive." }
        this.offsetValue = page * size
        this.limitValue = size
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                              BUDOWANIE SQL
    //------------------------------------------------------------------------------------------------------------------

    override fun buildSql(): String {
        if (selectClause.isBlank()) {
            throw IllegalStateException("Cannot build a SELECT query without a SELECT clause. Please call .select(...)")
        }
        if (fromClause.isNullOrBlank()) {
            // Jeśli są inne klauzule, które wymagają FROM, rzucamy wyjątek.
            if (whereCondition != null || groupByClause != null || orderByClause != null) {
                throw IllegalStateException("WHERE, GROUP BY, or ORDER BY clauses require a FROM clause.")
            }
        }
        if (havingClause != null && groupByClause == null) {
            throw IllegalStateException("HAVING clause requires a GROUP BY clause.")
        }

        val sqlBuilder = StringBuilder(buildWithClause())

        sqlBuilder.append("SELECT ").append(selectClause).append(" ")
        fromClause?.let { sqlBuilder.append("FROM ").append(it).append(" ") }
        whereCondition?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("WHERE ").append(it).append(" ") }
        groupByClause?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("GROUP BY ").append(it).append(" ") }
        havingClause?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("HAVING ").append(it).append(" ") }
        orderByClause?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("ORDER BY ").append(it).append(" ") }
        limitValue?.takeIf { it > 0 }?.let { sqlBuilder.append("LIMIT ").append(it).append(" ") }
        offsetValue?.takeIf { it >= 0 }?.let { sqlBuilder.append("OFFSET ").append(it).append(" ") }

        return sqlBuilder.toString().trim()
    }
}