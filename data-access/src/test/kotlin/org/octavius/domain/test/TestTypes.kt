package org.octavius.domain.test

import kotlinx.datetime.LocalDateTime
import org.octavius.data.contract.PgType
import java.math.BigDecimal

@PgType
enum class TestStatus { Active, Inactive, Pending, NotStarted }
@PgType
enum class TestPriority { Low, Medium, High, Critical }
@PgType
enum class TestCategory { BugFix, Feature, Enhancement, Documentation }
@PgType
data class TestMetadata(
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Int,
    val tags: List<String>
)

// W Postgresie wszystkie pola są nullable
// Jednakże tutaj ? dodane jest tylko dla pola które faktycznie będzie nullem
@PgType
data class TestPerson(
    val name: String?,
    val age: Int,
    val email: String,
    val active: Boolean,
    val roles: List<String>
)

@PgType
data class TestTask(
    val id: Int,
    val title: String,
    val description: String,
    val status: TestStatus,
    val priority: TestPriority,
    val category: TestCategory,
    val assignee: TestPerson,
    val metadata: TestMetadata,
    val subtasks: List<String>,
    val estimatedHours: BigDecimal
)

@PgType
data class TestProject(
    val name: String,
    val description: String,
    val status: TestStatus,
    val teamMembers: List<TestPerson>,
    val tasks: List<TestTask>,
    val metadata: TestMetadata,
    val budget: BigDecimal
)