# Octavius Database

[![API Documentation](https://img.shields.io/badge/KDoc-database--api-blue)](https://polskianonim.github.io/OctaviusFramework/database-api/)
[![Core Documentation](https://img.shields.io/badge/KDoc-database--core-blue)](https://polskianonim.github.io/OctaviusFramework/database-core/)

An un-opinionated, hyper-pragmatic data access layer for Kotlin & PostgreSQL.

It's not an ORM. It's an **Anti-ORM** — embracing the power of SQL combined with Kotlin's type safety.

## Philosophy

1. **The Query is King.** Your SQL query dictates the shape of data, not the other way around.
2. **The Object is a Vessel.** A `data class` is simply a type-safe container for query results.
3. **Explicitness over Magic.** No lazy-loading, no session management, no dirty checking.

## Quick Start

```kotlin
// Define your data class
data class User(val id: Int, val name: String, val email: String)

// Query the database
val users = dataAccess.select("id", "name", "email")
    .from("users")
    .where("active = true")
    .orderBy("name")
    .toListOf<User>()
```

## Configuration

```kotlin
// Option 1: From properties file
val config = DatabaseConfig.loadFromFile("database.properties")
val dataAccess = OctaviusDatabase.fromConfig(config)

// Option 2: Direct configuration
val dataAccess = OctaviusDatabase.fromConfig(
    DatabaseConfig(
        dbUrl = "jdbc:postgresql://localhost:5432/mydb",
        dbUsername = "user",
        dbPassword = "pass",
        dbSchemas = listOf("public"),
        packagesToScan = listOf("com.myapp.domain")
    )
)

// Option 3: From existing DataSource (for framework integration)
val dataAccess = OctaviusDatabase.fromDataSource(dataSource, ...)
```

## Query Builders

```kotlin
// SELECT
val users = dataAccess.select("id", "name")
    .from("users")
    .where("age > :min_age")
    .orderBy("name ASC")
    .limit(10)
    .toListOf<User>("min_age" to 18)

// INSERT with RETURNING
val newId = dataAccess.insertInto("users")
    .values(mapOf("name" to "John", "age" to 30))
    .returning("id")
    .toField<Int>()

// UPDATE
dataAccess.update("users")
    .setExpression("age", "age + 1")
    .where("id = :id")
    .execute("id" to 42)

// DELETE
dataAccess.deleteFrom("users")
    .where("status = 'INACTIVE'")
    .execute()
```

## Safe Dynamic Filters with QueryFragment

Build complex, conditional WHERE clauses without SQL injection risks:

```kotlin
fun buildFilters(name: String?, minAge: Int?, status: Status?): QueryFragment {
    val fragments = mutableListOf<QueryFragment>()

    name?.let {
        fragments += QueryFragment("name ILIKE :name", mapOf("name" to "%$it%"))
    }
    minAge?.let {
        fragments += QueryFragment("age >= :minAge", mapOf("minAge" to it))
    }
    status?.let {
        fragments += QueryFragment("status = :status", mapOf("status" to it))
    }

    return fragments.join(" AND ")
}

// Usage
val filter = buildFilters(name = "John", minAge = 18, status = null)
val users = dataAccess.select("*")
    .from("users")
    .where(filter.sql)
    .toListOf<User>(filter.params)
```

## Transaction Plans

Execute multi-step operations atomically, with dependencies between steps:

```kotlin
val plan = TransactionPlan()

// Step 1: Insert user, get handle to future ID
val userIdHandle = plan.add(
    dataAccess.insertInto("users")
        .values(mapOf("name" to "Jane", "email" to "jane@example.com"))
        .returning("id")
        .asStep()
        .toField<Int>()
)

// Step 2: Use the handle to reference future value
plan.add(
    dataAccess.insertInto("profiles")
        .values(mapOf("user_id" to userIdHandle.field(), "bio" to "Hello!"))
        .asStep()
        .execute()
)

// Execute atomically
dataAccess.executeTransactionPlan(plan)
```

## Type Mapping

Automatic bi-directional conversion between PostgreSQL and Kotlin types:

```kotlin
// PostgreSQL COMPOSITE -> Kotlin data class
@PgComposite
data class Address(val street: String, val city: String, val zip: String)

// PostgreSQL ENUM -> Kotlin enum
@PgEnum
enum class OrderStatus { Pending, Shipped, Delivered }
```

## Polymorphism with dynamic_dto

Store different object types in a single column — fully type-safe:

```kotlin
@DynamicallyMappable(typeName = "feature_flag")
@Serializable
enum class FeatureFlag {
    @SerialName("dark_theme") DarkTheme,
    @SerialName("new_dashboard") NewDashboard
}

// Write
dataAccess.update("user_settings")
    .setValue("flags")
    .where("id = 1")
    .execute("flags" to listOf(FeatureFlag.DarkTheme))

// Read — automatically converted back
val flags = dataAccess.select("flags")
    .from("user_settings")
    .where("id = 1")
    .toField<List<FeatureFlag>>()
```

## Architecture

| Module | Description |
|--------|-------------|
| `database/api` | Kotlin Multiplatform module with public API, interfaces, and annotations. No JVM dependencies. |
| `database/core` | JVM implementation using Spring JDBC and HikariCP. |
