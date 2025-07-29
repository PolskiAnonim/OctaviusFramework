package org.octavius.database

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject

class KotlinToPostgresConverterTest {

    private val converter = KotlinToPostgresConverter()

    enum class TestStatus {
        Active, Inactive, Pending
    }

    data class TestPerson(val name: String, val age: Int, val active: Boolean)

    @Test
    fun `expandParametersInQuery should handle simple parameters`() {
        val sql = "SELECT * FROM users WHERE id = :id AND name = :name"
        val params = mapOf("id" to 123, "name" to "John")

        val result = converter.expandParametersInQuery(sql, params)

        assertThat(result.expandedSql).isEqualTo("SELECT * FROM users WHERE id = :id AND name = :name")
        assertThat(result.expandedParams).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should handle null parameters`() {
        val sql = "SELECT * FROM users WHERE id = :id"
        val params = mapOf("id" to null)

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("SELECT * FROM users WHERE id = :id").isEqualTo(result.expandedSql)
        assertThat(mapOf("id" to null)).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should expand array parameters`() {
        val sql = "SELECT * FROM users WHERE id = ANY(:ids)"
        val params = mapOf("ids" to listOf(1, 2, 3))

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("SELECT * FROM users WHERE id = ANY(ARRAY[:ids_p1, :ids_p2, :ids_p3])").isEqualTo(result.expandedSql)
        assertThat(mapOf("ids_p1" to 1, "ids_p2" to 2, "ids_p3" to 3)).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should handle empty arrays`() {
        val sql = "SELECT * FROM users WHERE id = ANY(:ids)"
        val params = mapOf("ids" to emptyList<Int>())

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("SELECT * FROM users WHERE id = ANY('{}')").isEqualTo(result.expandedSql)
        assertThat(emptyMap<String, Any?>()).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should expand nested arrays`() {
        val sql = "SELECT * FROM test WHERE matrix = :matrix"
        val params = mapOf("matrix" to listOf(listOf(1, 2), listOf(3, 4)))

        val result = converter.expandParametersInQuery(sql, params)

        assertThat(
            "SELECT * FROM test WHERE matrix = ARRAY[ARRAY[:matrix_p1_p1, :matrix_p1_p2], ARRAY[:matrix_p2_p1, :matrix_p2_p2]]"
        ).isEqualTo(result.expandedSql)
        assertThat(
            mapOf(
                "matrix_p1_p1" to 1,
                "matrix_p1_p2" to 2,
                "matrix_p2_p1" to 3,
                "matrix_p2_p2" to 4
            )).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should expand enum parameters`() {
        val sql = "SELECT * FROM users WHERE status = :status"
        val params = mapOf("status" to TestStatus.Active)

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("SELECT * FROM users WHERE status = :status").isEqualTo(result.expandedSql)
        assertThat(1).isEqualTo(result.expandedParams.size)

        val pgObject = result.expandedParams["status"] as PGobject
        assertThat("test_status").isEqualTo(pgObject.type)
        assertThat("ACTIVE").isEqualTo(pgObject.value)
    }

    @Test
    fun `expandParametersInQuery should expand data class parameters`() {
        val sql = "INSERT INTO people VALUES (:person)"
        val person = TestPerson("John", 30, true)
        val params = mapOf("person" to person)

        val result = converter.expandParametersInQuery(sql, params)

        assertThat(
            "INSERT INTO people VALUES (ROW(:person_f1, :person_f2, :person_f3)::test_person)"
        ).isEqualTo(result.expandedSql)
        assertThat(
            mapOf(
                "person_f1" to "John",
                "person_f2" to 30,
                "person_f3" to true
            )).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should expand JSON parameters`() {
        val sql = "INSERT INTO documents (data) VALUES (:data)"
        val jsonData = JsonObject(
            mapOf(
                "name" to JsonPrimitive("test"),
                "value" to JsonPrimitive(42)
            )
        )
        val params = mapOf("data" to jsonData)

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("INSERT INTO documents (data) VALUES (:data)").isEqualTo(result.expandedSql)
        assertThat(1).isEqualTo(result.expandedParams.size)

        val pgObject = result.expandedParams["data"] as PGobject
        assertThat("jsonb").isEqualTo(pgObject.type)
        assertThat(pgObject.value!!).contains("\"name\":\"test\"", "\"value\":42")
    }

    @Test
    fun `expandParametersInQuery should handle arrays of enums`() {
        val sql = "SELECT * FROM users WHERE status = ANY(:statuses)"
        val params = mapOf("statuses" to listOf(TestStatus.Active, TestStatus.Pending))

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("SELECT * FROM users WHERE status = ANY(ARRAY[:statuses_p1, :statuses_p2])").isEqualTo(result.expandedSql)
        assertThat(2).isEqualTo(result.expandedParams.size)

        val status1 = result.expandedParams["statuses_p1"] as PGobject
        assertThat("test_status").isEqualTo(status1.type)
        assertThat("ACTIVE").isEqualTo(status1.value)

        val status2 = result.expandedParams["statuses_p2"] as PGobject
        assertThat("test_status").isEqualTo(status2.type)
        assertThat("PENDING").isEqualTo(status2.value)
    }

    @Test
    fun `expandParametersInQuery should handle arrays of data classes`() {
        val sql = "INSERT INTO people_batch VALUES (UNNEST(:people))"
        val people = listOf(
            TestPerson("John", 30, true),
            TestPerson("Jane", 25, false)
        )
        val params = mapOf("people" to people)

        val result = converter.expandParametersInQuery(sql, params)

        assertThat(
            "INSERT INTO people_batch VALUES (UNNEST(ARRAY[ROW(:people_p1_f1, :people_p1_f2, :people_p1_f3)::test_person, ROW(:people_p2_f1, :people_p2_f2, :people_p2_f3)::test_person]))"
        ).isEqualTo(result.expandedSql)
        assertThat(
            mapOf(
                "people_p1_f1" to "John",
                "people_p1_f2" to 30,
                "people_p1_f3" to true,
                "people_p2_f1" to "Jane",
                "people_p2_f2" to 25,
                "people_p2_f3" to false
            )).isEqualTo(result.expandedParams)
    }

    @Test
    fun `expandParametersInQuery should handle multiple parameters of different types`() {
        val sql = "SELECT * FROM users WHERE id = ANY(:ids) AND status = :status AND person = :person"
        val params = mapOf(
            "ids" to listOf(1, 2, 3),
            "status" to TestStatus.Active,
            "person" to TestPerson("John", 30, true)
        )

        val result = converter.expandParametersInQuery(sql, params)

        assertThat(
            "SELECT * FROM users WHERE id = ANY(ARRAY[:ids_p1, :ids_p2, :ids_p3]) AND status = :status AND person = ROW(:person_f1, :person_f2, :person_f3)::test_person"
        ).isEqualTo(result.expandedSql)

        // Sprawdź że wszystkie parametry są obecne
        assertThat(result.expandedParams).containsKey("ids_p1")
        assertThat(result.expandedParams).containsKey("ids_p2")
        assertThat(result.expandedParams).containsKey("ids_p3")
        assertThat(result.expandedParams).containsKey("status")
        assertThat(result.expandedParams).containsKey("person_f1")
        assertThat(result.expandedParams).containsKey("person_f2")
        assertThat(result.expandedParams).containsKey("person_f3")
    }

    @Test
    fun `expandParametersInQuery should ignore parameters not present in SQL`() {
        val sql = "SELECT * FROM users WHERE id = :id"
        val params = mapOf(
            "id" to 123,
            "unused_param" to "value"
        )

        val result = converter.expandParametersInQuery(sql, params)

        assertThat("SELECT * FROM users WHERE id = :id").isEqualTo(result.expandedSql)
        assertThat(
            mapOf("id" to 123, "unused_param" to "value")
        ).isEqualTo(result.expandedParams)
    }
}