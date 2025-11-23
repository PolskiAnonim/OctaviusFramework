# Database Layer

Advanced PostgreSQL database abstraction for Kotlin with automatic type mapping and transaction support.

## Modules

- **api/** - Common interfaces and data types (multiplatform compatible)
- **core/** - JVM implementation with PostgreSQL driver

## Features

- **TypeRegistry** - Automatic scanning of PostgreSQL schemas to map composite types to Kotlin data classes
- **Dynamic DTO Mapping** - Runtime mapping of database rows to Kotlin objects using `@PgType` and `@DynamicallyMappable` annotations
- **Transaction Plans** - Multi-step transactions with dependencies between steps
- **Query Builders** - Type-safe query construction with streaming support
- **Converters** - Bidirectional conversion between Kotlin and PostgreSQL types

## Requirements

- PostgreSQL 17+
- Kotlin 2.0+

## Usage

TODO: Add usage examples

## Architecture

TODO: Add architecture diagram