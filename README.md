# Octavius Framework

<div align="center">

**A modular desktop application for managing media collections, built with custom-engineered frameworks**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Octavius Database](https://img.shields.io/badge/Octavius_Database-v1.0-orange)](https://github.com/PolskiAnonim/OctaviusDatabase)

</div>

---

## Overview

Octavius is a Kotlin Multiplatform desktop application for tracking manga, light novels, and game collections. What makes it unique is that it's built entirely on **custom-engineered frameworks** — a form engine, report engine, and database access layer — designed from scratch to solve real problems without the overhead of traditional solutions.

## Highlights

| Component                                                                 | Description                                                                                                                        |
|---------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| **[Octavius Database](https://github.com/PolskiAnonim/OctaviusDatabase)** | *External Library.* An "Anti-ORM" — SQL-first data access with automatic type mapping for PostgreSQL composites, enums, and arrays |
| **[Form Engine](form-engine/)**                                           | Declarative form builder with dependencies, validation, and repeatable sections                                                    |
| **[Report Engine](report-engine/)**                                       | Dynamic data tables with filtering, sorting, column management, and persistent layouts                                             |
| **Browser Extension**                                                     | Kotlin/JS extension for importing data from external sources.                                                                      |

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

**Backend / Data**
- Octavius Database (Custom JDBC wrapper)
- HikariCP
- Ktor (API server)
- kotlinx-serialization

</td>
</tr>
</table>

## Architecture

The project follows a modular distributed architecture. The Core Database layer is developed as a separate library.

```
Octavius/
├── desktop-app/             # Main application entry point
├── core/                    # Domain models, localization, utilities
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

*Note: The `database` module is externalized and imported via GitHub Packages.*

## Custom Frameworks

### Form Engine

A data-driven form framework supporting:

- **Control types**: Primitives, dropdowns (enum/database), string lists, sections
- **Repeatable sections**: Dynamic row management with add/remove
- **Dependencies**: Show/hide controls based on other values
- **Validation**: Multi-level validation with business rules

[Learn more →](form-engine/)

### Report Engine

Configurable data tables with:

- **Column management**: Drag & drop, visibility toggles, reordering
- **Type-specific filters**: String, number, enum, boolean, date
- **Multi-column sorting** with state persistence
- **Layout presets**: Save and load table configurations

[Learn more →](report-engine/)

### Database Layer (Octavius Database)

SQL-first approach with automatic mapping (imported as a library):

```kotlin
// Your query shapes the result — not the ORM
val books = dataAccess.select("id", "title", "author", "status")
    .from("books")
    .where("status = :status")
    .orderBy("title")
    .toListOf<Book>("status" to BookStatus.Reading)
```

[See Repository →](https://github.com/PolskiAnonim/OctaviusDatabase)

## Getting Started

### Requirements

- JDK 24+
- PostgreSQL 17+
- Database `octavius` created locally

### Dependency Setup (Choose one)

#### Option A: Local Development (Recommended)
Since the database layer is a separate repository, you need to build it first:

1. Clone **[OctaviusDatabase](https://github.com/PolskiAnonim/OctaviusDatabase)**
2. Run `./gradlew publishToMavenLocal` in the database directory.
3. Build this project (it will pick up the library from your local Maven cache).

#### Option B: Remote Fetch (CI / Fresh Install)
If you don't want to build the database library locally, you need a `GITHUB_TOKEN` with `read:packages` scope to download it from GitHub Packages.

Add to `~/.gradle/gradle.properties`:
```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

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

- **Type-safe translation system** with auto-generated accessors (`Tr`)
- JSON-based translations per module (`translations_pl.json`)
- Plural forms support (one/few/many)
- Build-time code generation: `./gradlew generateTranslationAccessors`

```kotlin
// Type-safe accessors instead of string keys
Tr.Action.save()                     // → "Zapisz"
Tr.Games.Form.category(5)            // → Plural form based on count
Tr.currentLanguage = "en"            // Runtime language switching
```
