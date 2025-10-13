// Możesz to umieścić w tym samym pakiecie co inne benchmarki
package org.octavius.performance

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.octavius.database.DatabaseConfig
import org.octavius.database.type.PostgresToKotlinConverter
import org.octavius.database.type.TypeRegistryLoader
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.ResultSet
import kotlin.system.measureTimeMillis

/**
 * Benchmark porównujący wydajność mapowania prostych typów.
 *
 * Mierzy narzut (overhead) wprowadzany przez `PostgresToKotlinConverter` (strategia getString())
 * w porównaniu do bezpośredniego użycia natywnych metod JDBC (getInt(), getTimestamp() itd.).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleTypeOverheadBenchmark {

    // --- Konfiguracja ---
    private val TOTAL_ROWS_TO_FETCH = 10000
    private val ITERATIONS = 20
    private val WARMUP_ITERATIONS = 10

    // --- Wyniki ---
    private val frameworkTimings = mutableListOf<Long>()
    private val rawJdbcTimings = mutableListOf<Long>()

    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    private lateinit var typesConverter: PostgresToKotlinConverter

    @BeforeAll
    fun setup() {
        println("--- KONFIGURACJA BENCHMARKU NARZUTU DLA PROSTYCH TYPÓW ---")
        val databaseConfig = DatabaseConfig.loadFromFile("test-database.properties")
        // 2. KRYTYCZNE ZABEZPIECZENIE (ASSERTION GUARD)
        val connectionUrl = databaseConfig.dbUrl
        val dbName = connectionUrl.substringAfterLast("/") // Wyciągamy nazwę bazy z URL-a

        // Sprawdzamy zarówno URL, jak i nazwę bazy, aby być podwójnie pewnym.
        // Można też sprawdzić hosta, port etc.
        if (!connectionUrl.contains("localhost:5432") || dbName != "octavius_test") {
            throw IllegalStateException(
                "ABORTING TEST! Attempting to run destructive tests on a non-test database. " +
                        "Connection URL: '$connectionUrl'. This is a safety guard to prevent data loss."
            )
        }
        println("Safety guard passed. Connected to the correct test database: $dbName")
        val hikariDataSource = HikariDataSource().apply {
            jdbcUrl = databaseConfig.dbUrl
            username = databaseConfig.dbUsername
            password = databaseConfig.dbPassword
            maximumPoolSize = 5 // Mała pula wystarczy, test jest jednowątkowy
        }

        jdbcTemplate = NamedParameterJdbcTemplate(hikariDataSource)

        // Potrzebujemy konwertera dla `FrameworkRowMapper`
        val typeRegistry = runBlocking {
            TypeRegistryLoader(jdbcTemplate, databaseConfig.packagesToScan, databaseConfig.dbSchemas).load()
        }
        typesConverter = PostgresToKotlinConverter(typeRegistry)

        try {
            val initSql = String(
                Files.readAllBytes(
                    Paths.get(
                        this::class.java.classLoader.getResource("init-simple-test-db.sql")!!.toURI()
                    )
                )
            )
            jdbcTemplate.jdbcTemplate.execute(initSql)
            println("Complex test DB schema and data initialized successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `run full benchmark comparison`() {
        val sql = "SELECT * FROM simple_type_benchmark LIMIT $TOTAL_ROWS_TO_FETCH"
        val rawMapper = RawJdbcRowMapper()
        val frameworkMapper = FrameworkRowMapper(typesConverter)

        // --- WARM-UP ---
        println("\n--- ROZGRZEWKA (x$WARMUP_ITERATIONS iteracji, wyniki ignorowane) ---")
        repeat(WARMUP_ITERATIONS) {
            jdbcTemplate.query(sql, rawMapper)
            jdbcTemplate.query(sql, frameworkMapper)
        }
        println("--- ROZGRZEWKA ZAKOŃCZONA ---\n")

        // --- POMIAR ---
        println("--- POMIAR (x$ITERATIONS iteracji dla $TOTAL_ROWS_TO_FETCH wierszy) ---")
        repeat(ITERATIONS) { i ->
            print("Iteracja ${i + 1}/$ITERATIONS...\r")

            // Mierz Raw JDBC
            val rawTime = measureTimeMillis {
                jdbcTemplate.query(sql, rawMapper)
            }
            rawJdbcTimings.add(rawTime)

            // Mierz Framework
            val frameworkTime = measureTimeMillis {
                jdbcTemplate.query(sql, frameworkMapper)
            }
            frameworkTimings.add(frameworkTime)
        }
        println("\n--- POMIAR ZAKOŃCZONY ---\n")
    }

    @AfterAll
    fun printResults() {
        val avgRaw = rawJdbcTimings.average()
        val avgFramework = frameworkTimings.average()
        val overheadMs = avgFramework - avgRaw
        val overheadPercent = (overheadMs / avgRaw) * 100

        println("\n--- OSTATECZNE WYNIKI PORÓWNANIA (średnia z $ITERATIONS iteracji) ---")
        println("=======================================================================")
        println("  Pobieranie i mapowanie $TOTAL_ROWS_TO_FETCH wierszy:")
        println("-----------------------------------------------------------------------")
        println("  - Raw JDBC (linia bazowa):  ${String.format("%.2f", avgRaw)} ms")
        println("  - Framework (getString()):    ${String.format("%.2f", avgFramework)} ms")
        println("-----------------------------------------------------------------------")
        println("  - Narzut (Overhead):        +${String.format("%.2f", overheadMs)} ms")
        println("  - Narzut procentowy:        +${String.format("%.1f", overheadPercent)}%")
        println("=======================================================================")
        println("\nInterpretacja: Narzut to dodatkowy czas, jaki abstrakcja potrzebuje")
        println("w porównaniu do najbardziej optymalnego, surowego kodu JDBC.")
    }
}

/**
 * RowMapper implementujący podejście "surowego" JDBC.
 * Używa specyficznych metod `get<Type>()` dla każdej kolumny po indeksie.
 * Służy jako punkt odniesienia (linia bazowa) do pomiaru narzutu.
 */
private class RawJdbcRowMapper : RowMapper<Map<String, Any?>> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>()
        // Używamy indeksów kolumn - to najszybsza metoda
        data["id"] = rs.getInt(1)
        data["int_val"] = rs.getInt(2)
        data["long_val"] = rs.getLong(3)
        data["text_val"] = rs.getString(4)
        data["ts_val"] = rs.getTimestamp(5)?.toLocalDateTime()
        data["bool_val"] = rs.getBoolean(6)
        data["numeric_val"] = rs.getBigDecimal(7)
        return data
    }
}

/**
 * RowMapper implementujący podejście frameworka Octavius.
 * Używa `getString()` dla wszystkich kolumn i deleguje konwersję do `PostgresToKotlinConverter`.
 */
private class FrameworkRowMapper(private val converter: PostgresToKotlinConverter) : RowMapper<Map<String, Any?>> {
    override fun mapRow(rs: ResultSet, rowNum: Int): Map<String, Any?> {
        val data = mutableMapOf<String, Any?>()
        val metaData = rs.metaData
        for (i in 1..metaData.columnCount) {
            val columnName = metaData.getColumnName(i)
            val columnType = metaData.getColumnTypeName(i)
            val rawValue = rs.getString(i)
            data[columnName] = converter.convert(rawValue, columnType)
        }
        return data
    }
}