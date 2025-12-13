# Report Engine

<div align="center">

**A dynamic data table framework for Compose Multiplatform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)

</div>

---

## Overview

Report Engine is a framework for building interactive data tables with filtering, sorting, pagination, and configurable layouts. Define your report structure once, and the engine provides a full-featured table UI with persistent user preferences.

## Features

- **Declarative Structure** — Define columns, filters, and actions in a builder
- **Type-Safe Columns** — String, Number, Boolean, Enum, DateTime, Interval
- **Smart Filtering** — Type-specific filters with SQL generation
- **Multi-Column Sorting** — Click headers to sort, with state persistence
- **Column Management** — Hide, reorder, resize columns via panel
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

### 2. Implement Data Manager

```kotlin
class BooksReportDataManager : ReportDataManager() {

    override suspend fun fetchData(
        query: QueryFragment,
        filters: Map<String, FilterData>,
        sorting: List<SortingInfo>,
        pagination: PaginationInfo
    ): ReportData {
        // Combine base query with active filters
        val filterFragment = buildFilterFragment(filters)
        val sortClause = buildSortClause(sorting)

        val finalQuery = """
            ${query.sql}
            ${if (filterFragment.sql.isNotEmpty()) "AND ${filterFragment.sql}" else ""}
            $sortClause
            LIMIT :limit OFFSET :offset
        """

        val params = query.params + filterFragment.params + mapOf(
            "limit" to pagination.pageSize,
            "offset" to pagination.offset
        )

        val rows = dataAccess.rawQuery(finalQuery)
            .toListOfMaps(params)

        val totalCount = dataAccess.select("COUNT(*)")
            .from("books")
            .where("archived = false")
            .toField<Long>()

        return ReportData(rows, totalCount)
    }
}
```

### 3. Render the Report

```kotlin
@Composable
fun BooksReportScreen() {
    ReportScreen(
        structureBuilder = BooksReportStructureBuilder(),
        dataManager = BooksReportDataManager()
    )
}
```

## Filter Types

Each column type has specialized filters:

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
- **Presets** — Save/load named configurations

```kotlin
// Configurations are persisted per report name
ReportConfigurationManager.save(reportName, config)
ReportConfigurationManager.load(reportName)
```

## Row Actions

```kotlin
// Context menu actions (shown on right-click or action button)
override fun buildRowActions() = listOf(
    ReportRowAction("edit", "Edit", Icons.Default.Edit) { row ->
        navigateTo(EditScreen(row["id"] as Int))
    },
    ReportRowAction("duplicate", "Duplicate", Icons.Default.ContentCopy) { row ->
        duplicateEntry(row["id"] as Int)
    },
    ReportRowAction("delete", "Delete", Icons.Default.Delete) { row ->
        showDeleteConfirmation(row["id"] as Int)
    }
)

// Quick action (shown as icon, triggered on double-click)
override fun buildDefaultRowAction() = ReportRowAction("open", "Open") { row ->
    navigateTo(DetailsScreen(row["id"] as Int))
}
```

## Architecture

```
report-engine/
├── component/
│   ├── ReportStructure.kt     # Report definition
│   ├── ReportState.kt         # Runtime state
│   ├── ReportHandler.kt       # Orchestrates lifecycle
│   ├── ReportScreen.kt        # Main Compose UI
│   └── ReportDataManager.kt   # Data fetching interface
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
