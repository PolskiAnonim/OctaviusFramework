package org.octavius.domain.test

import kotlinx.datetime.LocalDateTime
//import java.math.BigDecimal

enum class TestStatus { Active, Inactive, Pending, NotStarted }
enum class TestPriority { Low, Medium, High, Critical }
enum class TestCategory { BugFix, Feature, Enhancement, Documentation }

data class TestMetadata(
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Int,
    val tags: List<String>
)

// W Postgresie wszystkie pola są nullable
// Jednakże tutaj ? dodane jest tylko dla pola które faktycznie będzie nullem
data class TestPerson(
    val name: String?,
    val age: Int,
    val email: String,
    val active: Boolean,
    val roles: List<String>
)

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
    val estimatedHours: Double//BigDecimal - numeric konwerter traktuje jak Double
)

data class TestProject(
    val name: String,
    val description: String,
    val status: TestStatus,
    val teamMembers: List<TestPerson>,
    val tasks: List<TestTask>,
    val metadata: TestMetadata,
    val budget: Double//BigDecimal - numeric konwerter traktuje jak Double
)