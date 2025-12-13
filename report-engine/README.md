# Report Engine

<div align="center">

**A dynamic data table framework for Compose Multiplatform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![KDoc](https://img.shields.io/badge/KDoc-Documentation-blue)](https://polskianonim.github.io/OctaviusFramework/report-engine/)

</div>

---

## Overview

Report Engine is a framework for building interactive data tables with filtering, sorting, pagination, and configurable layouts. Define your report structure once, and the engine provides a full-featured table UI with persistent user preferences.

## Features

- **Declarative Structure** — Define columns, filters, and actions in a builder
- **Type-Safe Columns** — String, Number, Boolean, Enum, DateTime, Interval
- **Smart Filtering** — Type-specific filters with automatic SQL generation
- **Multi-Column Sorting** — Click headers to sort, with state persistence
- **Column Management** — Hide, reorder columns via management panel
- **Row Actions** — Context menus and quick actions per row
- **Layout Presets** — Save and load table configurations
- **Pagination** — Efficient loading of large datasets

## Column Types

| Type | Description | Filter |
|------|-------------|--------|
| `StringColumn` | Text data | Contains, starts with, equals |
| `NumberColumn` | Numeric data | Equals, greater/less than, range |
| `BooleanColumn` | True/false | Checkbox |
| `EnumColumn` | Enum values | Multi-select dropdown |
| `DateTimeColumn` | Dates and times | Date range picker |
| `IntervalColumn` | Duration/intervals | Range |
| `MultiRowColumn` | Lists in single cell | — |
| `ActionColumn` | Row action buttons | — |

## Quick Start

### 1. Define Report Structure

```kotlin
class BooksReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName() = "books"

    override fun buildQuery() = QueryFragment(
        """
        SELECT id, title, author, year, status, created_at
        FROM books
        WHERE archived = false
        """
    )

    override fun buildColumns() = mapOf(
        "title" to StringColumn(
            header = "Title",
            width = ColumnWidth.Weight(2f),
            filterable = true,
            sortable = true
        ),
        "author" to StringColumn(
            header = "Author",
            width = ColumnWidth.Weight(1.5f),
            filterable = true,
            sortable = true
        ),
        "year" to NumberColumn(
            header = "Year",
            width = ColumnWidth.Fixed(80.dp),
            filterable = true,
            sortable = true
        ),
        "status" to EnumColumn<BookStatus>(
            header = "Status",
            width = ColumnWidth.Fixed(120.dp),
            filterable = true,
            sortable = true
        ),
        "created_at" to DateTimeColumn(
            header = "Added",
            width = ColumnWidth.Fixed(150.dp),
            filterable = true,
            sortable = true
        )
    )

    override fun buildRowActions() = listOf(
        ReportRowAction("edit", "Edit") { row -> navigateToEdit(row["id"] as Int) },
        ReportRowAction("delete", "Delete") { row -> confirmDelete(row["id"] as Int) }
    )

    override fun buildDefaultRowAction() = ReportRowAction("open", "Open") { row ->
        navigateToDetails(row["id"] as Int)
    }

    override fun buildMainActions() = listOf(
        ReportMainAction("add", "Add Book") { navigateToNew() }
    )
}
```

### 2. Render the Report

```kotlin
@Composable
fun BooksReportScreen() {
    ReportScreen(
        structureBuilder = BooksReportStructureBuilder()
    )
}
```

That's it! The `ReportDataManager` is created automatically from the structure and handles all query building, filtering, sorting, and pagination.

## Column Width

Columns can have fixed or flexible widths:

```kotlin
// Fixed width in dp
"id" to NumberColumn(
    header = "ID",
    width = ColumnWidth.Fixed(60.dp),
    ...
)

// Flexible width with weight
"title" to StringColumn(
    header = "Title",
    width = ColumnWidth.Weight(2f),  // Takes 2x space compared to weight 1
    ...
)
```

## Filter Types

Each column type has specialized filters that automatically generate SQL:

```kotlin
// String filter: contains, starts with, ends with, equals
"title" to StringColumn(header = "Title", filterable = true, ...)

// Number filter: equals, not equals, greater/less than, between
"price" to NumberColumn(header = "Price", filterable = true, ...)

// Enum filter: multi-select from enum values
"status" to EnumColumn<Status>(header = "Status", filterable = true, ...)

// Boolean filter: true/false/any
"active" to BooleanColumn(header = "Active", filterable = true, ...)

// DateTime filter: date range picker
"createdAt" to DateTimeColumn(header = "Created", filterable = true, ...)
```

## Column Management

Users can customize their view through the column management panel:

- **Visibility** — Toggle columns on/off
- **Order** — Drag and drop to reorder
- **Sorting** — Multi-column sort with direction

Configurations are persisted per report name:

```kotlin
// Automatic - uses reportName from structure builder
ReportConfigurationManager.save(reportName, config)
ReportConfigurationManager.load(reportName)
```

## Row Actions

```kotlin
// Context menu actions (shown on right-click or action column)
override fun buildRowActions() = listOf(
    ReportRowAction("edit", "Edit") { row ->
        navigateTo(EditScreen(row["id"] as Int))
    },
    ReportRowAction("duplicate", "Duplicate") { row ->
        duplicateEntry(row["id"] as Int)
    },
    ReportRowAction("delete", "Delete") { row ->
        showDeleteConfirmation(row["id"] as Int)
    }
)

// Default action (triggered on double-click, shown as quick action icon)
override fun buildDefaultRowAction() = ReportRowAction("open", "Open") { row ->
    navigateTo(DetailsScreen(row["id"] as Int))
}

// Main actions (shown in toolbar, e.g., "Add new")
override fun buildMainActions() = listOf(
    ReportMainAction("add", "Add New") { navigateTo(NewScreen()) }
)
```

## Architecture

```
report-engine/
├── component/
│   ├── ReportStructure.kt     # Report definition
│   ├── ReportState.kt         # Runtime state
│   ├── ReportHandler.kt       # Orchestrates lifecycle
│   ├── ReportScreen.kt        # Main Compose UI
│   └── ReportDataManager.kt   # Query building & data fetching
│
├── column/
│   ├── ReportColumn.kt        # Base column class
│   └── type/                  # Column implementations
│
├── filter/
│   ├── Filter.kt              # Base filter class
│   ├── data/                  # Filter state classes
│   └── type/                  # Filter implementations
│
├── configuration/             # Layout persistence
├── management/                # Column management panel
└── ui/                        # Table components
```
