package org.octavius.novels.database

import androidx.compose.foundation.pager.PageSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.domain.Novel
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.util.Converters.camelToSnakeCase
import java.sql.Connection
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

val LocalDatabase = compositionLocalOf<DatabaseManager> { error("No DatabaseManager found!") }

class DatabaseManager(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String
) {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@DatabaseManager.jdbcUrl
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)
    }

    private fun getConnection(): Connection = dataSource.connection

    fun <T: Any> getDataForPage(tableName: String, currentPage: Int, pageSize: Int, searchQuery: String, resultClass: KClass<T>): Pair<List<T>, Long> {
        var totalElements: Long;
        val offset = (currentPage - 1) * pageSize;
        val results = mutableListOf<T>()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT count(*) AS total FROM $tableName").use { resultSet ->
                    resultSet.next()
                    totalElements = resultSet.getLong("total")
                }
                val sql = "SELECT * FROM $tableName $searchQuery LIMIT $pageSize OFFSET $offset"
                statement.executeQuery(sql).use { resultSet ->
                    val constructor = resultClass.primaryConstructor!!
                    val parameters = constructor.parameters

                    while (resultSet.next()) {
                        val args = parameters.associateWith { param ->
                            val columnName = param.name!!
                            when (param.type.classifier) {
                                String::class -> resultSet.getString(camelToSnakeCase(columnName))
                                Int::class -> resultSet.getInt(camelToSnakeCase(columnName))
                                Long::class -> resultSet.getLong(camelToSnakeCase(columnName))
                                Double::class -> resultSet.getDouble(camelToSnakeCase(columnName))
                                Boolean::class -> resultSet.getBoolean(camelToSnakeCase(columnName))
                                NovelStatus::class -> NovelStatus.valueOf(resultSet.getString(camelToSnakeCase(columnName)))
                                List::class -> listOf(resultSet.getArray(camelToSnakeCase(columnName)).array)
                                else -> throw IllegalArgumentException("Unsupported type: ${param.type}")
                            }
                        }

                        results.add(constructor.callBy(args))
                    }
                }
            }
        }
        return Pair<List<T>, Long>(results, totalElements)
    }
}