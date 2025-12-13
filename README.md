# Octavius Framework

<div align="center">

**A modular desktop application for managing media collections, built with custom-engineered frameworks**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![KDoc](https://img.shields.io/badge/KDoc-Documentation-blue)](https://polskianonim.github.io/OctaviusFramework/)

</div>

---

## Overview

Octavius is a Kotlin Multiplatform desktop application for tracking manga, light novels, and game collections. What makes it unique is that it's built entirely on **custom-engineered frameworks** — a form engine, report engine, and database access layer — designed from scratch to solve real problems without the overhead of traditional solutions.

## Highlights

| Component | Description |
|-----------|-------------|
| **[Database Layer](database/)** | An "Anti-ORM" — SQL-first data access with automatic type mapping for PostgreSQL composites, enums, and arrays |
| **Form Engine** | Declarative form builder with dependencies, validation, and repeatable sections |
| **Report Engine** | Dynamic data tables with filtering, sorting, column management, and persistent layouts |
| **Browser Extension** | Kotlin/JS extension for importing data from external sources |

## Tech Stack

<table>
<tr>
<td>

**Core**
- Kotlin Multiplatform
- Compose Multiplatform
- PostgreSQL 17+
- Material 3

</td>
<td>

**Backend**
- Spring JDBC
- HikariCP
- Ktor (API server)
- kotlinx-serialization

</td>
</tr>
</table>

## Architecture

```
Octavius/
├── desktop-app/             # Main application entry point
├── core/                    # Domain models, localization, utilities
│
├── database/                # Custom SQL-first data access layer
│   ├── api/                 # Multiplatform API & annotations
│   └── core/                # JVM implementation
│
├── form-engine/             # Declarative form framework
├── report-engine/           # Dynamic table framework
├── ui-core/                 # Shared UI components & navigation
│
├── feature-asian-media/     # Manga, novels, manhwa tracking
├── feature-games/           # Game collection management
├── feature-settings/        # Application settings
│
├── api-server/              # REST API for browser extension
└── browser-extension/       # Kotlin/JS Chrome extension
```

## Custom Frameworks

### Form Engine

A data-driven form framework supporting:

- **Control types**: Primitives, dropdowns (enum/database), string lists, sections
- **Repeatable sections**: Dynamic row management with add/remove
- **Dependencies**: Show/hide controls based on other values
- **Validation**: Multi-level validation with business rules

### Report Engine

Configurable data tables with:

- **Column management**: Drag & drop, visibility toggles, reordering
- **Type-specific filters**: String, number, enum, boolean, date
- **Multi-column sorting** with state persistence
- **Layout presets**: Save and load table configurations

### Database Layer

SQL-first approach with automatic mapping:

```kotlin
// Your query shapes the result — not the ORM
val books = dataAccess.select("id", "title", "author", "status")
    .from("books")
    .where("status = :status")
    .orderBy("title")
    .toListOf<Book>("status" to BookStatus.Reading)
```

[Learn more →](database/)

## Getting Started

### Requirements

- JDK 24+
- PostgreSQL 17+
- Database `octavius` initialized with `baza.sql`

### Run

```bash
./gradlew run
```

### Build Browser Extension

```bash
./gradlew assembleBrowserExtension
# Output: browser-extension/build/extension/
```

## Localization

- JSON-based translations per module (`translations_pl.json`)
- Plural forms support (one/few/many)
- Compile-time validation: `./gradlew validateTranslations`
