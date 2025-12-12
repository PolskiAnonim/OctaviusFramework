# Octavius Database: An Un-opinionated, Hyper-Pragmatic Data Access Layer for Kotlin & PostgreSQL

**Octavius Database** is a data access framework for Kotlin, born from a fundamental disagreement with the magic and complexity of traditional ORMs. It provides a powerful, intuitive, and un-opinionated way to interact with your PostgreSQL database, putting you—the developer—back in control.

It's not an ORM. It's an **Anti-ORM**. It embraces the power of SQL and combines it with the safety and elegance of Kotlin's type system, giving you the best of both worlds.

## Core Philosophy

This library is built on a few simple, powerful principles:

1.  **The Query is King.** The shape and content of your data objects are dictated by your SQL query, not the other way around. You have absolute control over joins, aggregations, and performance.
2.  **The Object is a Vessel.** A `data class` is simply a type-safe container for the results of your query. The framework's job is to fill this vessel with perfect fidelity, no questions asked.
3.  **Explicitness over Magic.** No hidden lazy-loading, no session management, no dirty checking. Every database operation is an explicit, predictable action. You get what you ask for—nothing more, nothing less.

## Key Features

*   **Fluent & Intuitive Query Builders:** A "clause-gluing" approach that provides structure without sacrificing the power of raw SQL strings.
*   **Powerful Automatic Type Mapping:** Robust, bi-directional conversion between PostgreSQL types (including `COMPOSITE`, `ENUM`, `ARRAY`) and Kotlin `data class`/`enum class` objects.
*   **Declarative Transaction Management:** Build complex, atomic operations as a `TransactionPlan`—a list of steps with dependencies, where the result of one step can be used as an input for the next.
*   **First-Class Polymorphism in PostgreSQL:** A groundbreaking implementation of `dynamic_dto` that allows you to store and retrieve even lists of different object types in a single database column, fully type-safe.
*   **Streaming & Asynchronous Support:** Process large result sets efficiently with `forEach` streaming, or run queries asynchronously in a Coroutine scope.
*   **Safe Dynamic Filters:** Build complex, conditional `WHERE` clauses with `QueryFragment` without risking SQL injection or operator precedence errors.
*   **Pragmatic & Modular Architecture:** A clean separation between the multiplatform `api` module (for sharing data models) and the JVM-based `core` implementation.


## Configuration and Initialization

### Three ways to initialize:

**Two from DatabaseConfig (standalone):**

`database.properties` file in resources:
```properties
db.url=jdbc:postgresql://localhost:5432/my_app
db.username=user
db.password=pass
db.schemas=public, my_schema
db.packagesToScan=com.myapp.domain
```
```kotlin
import org.octavius.data.DataAccess
import org.octavius.database.config.DatabaseConfig
import org.octavius.database.OctaviusDatabase

val config = DatabaseConfig.loadFromFile("database.properties")
val dataAccess: DataAccess = OctaviusDatabase.fromConfig(config)
```
Or directly
```kotlin
import org.octavius.data.DataAccess
import org.octavius.database.config.DatabaseConfig
import org.octavius.database.OctaviusDatabase

OctaviusDatabase.fromConfig(
            DatabaseConfig(
                dbUrl = "url",
                dbUsername = "postgres",
                dbPassword = "postgres",
                dbSchemas = listOf("public"),
                setSearchPath = true,
                packagesToScan = listOf(),
                dynamicDtoStrategy = DynamicDtoSerializationStrategy.AUTOMATIC_WHEN_UNAMBIGUOUS
            )
        )
```
And one from DataSource (for integration):
```kotlin
import org.octavius.data.DataAccess
import org.octavius.database.config.DatabaseConfig
import org.octavius.database.OctaviusDatabase

val config = DatabaseConfig.loadFromFile("database.properties")
val dataAccess: DataAccess = OctaviusDatabase.fromDataSource(
        dataSource = datasource,
        packagesToScan = listOf(),
        dbSchema = listOf(),
        dynamicDtoStrategy = DynamicDtoSerializationStrategy.AUTOMATIC_WHEN_UNAMBIGUOUS
)
```

## Core Concepts

### Query Builders

Build any query type with a fluent, readable API. The builders handle the structure, you provide the clauses.

```kotlin
// SELECT
val users = dataAccess.select("id", "name")
    .from("users")
    .where("age > :min_age")
    .orderBy("name ASC")
    .limit(10)
    .toListOf<User>("min_age" to 18)

// INSERT
val newUserId = dataAccess.insertInto("users")
    .values(mapOf("name" to "John Doe", "age" to 30))
    .returning("id")
    .toField<Int>()

// UPDATE
val updatedRows = dataAccess.update("users")
    .setExpression("age", "age + 1")
    .where("id = :id")
    .execute("id" to 42)

// DELETE
val deletedRows = dataAccess.deleteFrom("users")
    .where("status = 'INACTIVE'")
    .execute()
```

### Transaction Plans

For complex, multi-step operations, `TransactionPlan` is your ultimate weapon. Define a series of steps and execute them in a single, atomic transaction.

**Scenario:** Create a new user, and immediately use their new ID to create their profile.

```kotlin
val plan = TransactionPlan()

// Step 1: Insert the user and get a "handle" to its future ID.
val userData = mapOf("name" to "Jane Doe", "email" to "jane@example.com")
val userIdHandle = plan.add(
    dataAccess.insertInto("users")
        .values(userData)
        .returning("id")
        .asStep() // Convert the query to a transaction step
        .toField<Int>(userData)
)

// Step 2: Insert the profile, using the handle to reference the future user ID.
val profileData = mapOf(
    "bio" to "Loves Kotlin and PostgreSQL",
    "user_id" to userIdHandle.field() // Reference a future value.
)
plan.add(
    dataAccess.insertInto("profiles")
        .values(profileData)
        .asStep()
        .execute(profileData)
)

// Execute the entire plan atomically.
val result = dataAccess.executeTransactionPlan(plan)
```

### First-Class Polymorphism

Leverage the `dynamic_dto` type to store different kinds of objects in the same column or array, enabling powerful patterns like **Soft Enums**.

A **Soft Enum** is a Kotlin `enum` that is not backed by a rigid `ENUM` type in PostgreSQL, allowing you to add new values without database migrations.

```kotlin
// 1. Define your enum with annotations
@DynamicallyMappable(typeName = "feature_flag")
@Serializable
enum class FeatureFlag {
    @SerialName("dark_theme") DarkTheme,
    @SerialName("new_dashboard") NewDashboard
}

// 2. Your database table has a column of type `dynamic_dto[]`
// CREATE TABLE user_settings (id SERIAL, flags dynamic_dto[]);

// 3. Write and read it like any other list!
val flagsToSave: List<FeatureFlag> = listOf(FeatureFlag.DarkTheme, FeatureFlag.NewDashboard)

// The framework automatically converts the list to `dynamic_dto[]`
dataAccess.update("user_settings")
    .setValue("flags")
    .where("id = 1")
    .execute("flags" to flagsToSave)

// And automatically converts it back!
val savedFlags = dataAccess.select("flags")
    .from("user_settings")
    .where("id = 1")
    .toField<List<FeatureFlag>>()
```

## Architecture

The project is split into two main modules:
*   `./database/api`: A Kotlin Multiplatform (`commonMain`) module that defines the public API, interfaces, annotations, and data transfer objects. It has no dependencies on the JVM or Spring.
*   `./database/core`: A JVM module that contains the concrete implementation of the API. It uses Spring JDBC, HikariCP, and a lot of diabolical magic to bring the API to life.

[![Documentation](https://img.shields.io/badge/KDoc-documentation-blue)]( https://polskianonim.github.io/OctaviusFramework/)