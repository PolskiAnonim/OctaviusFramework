# Form Engine

<div align="center">

**A declarative, data-driven form framework for Compose Multiplatform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)

</div>

---

## Overview

Form Engine is a framework for building complex, interactive forms in Compose. Define your form structure declaratively with a schema, and the engine handles rendering, validation, state management, and data binding automatically.

## Features

- **Declarative Schema** — Define forms as data, not UI code
- **Rich Control Types** — Primitives, dropdowns, sections, repeatable rows
- **Dependencies** — Show/hide or require controls based on other values
- **Actions** — Trigger logic on value changes
- **Validation** — Multi-level validation with custom rules
- **Two-way Binding** — Automatic sync between UI and data

## Control Types

| Category | Controls |
|----------|----------|
| **Primitive** | `StringControl`, `IntegerControl`, `DoubleControl`, `BigDecimalControl`, `BooleanControl` |
| **DateTime** | `DateTimeControl`, `IntervalControl` |
| **Selection** | `EnumControl`, `DatabaseControl` (dropdown from query) |
| **Collection** | `StringListControl` (dynamic string array) |
| **Container** | `SectionControl` (grouping with collapse) |
| **Repeatable** | `RepeatableControl` (dynamic rows with add/remove) |
| **Action** | `ButtonControl` |

## Quick Start

### 1. Define a Schema

```kotlin
class BookFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineControls() = mapOf(
        "title" to StringControl(label = "Title", required = true),
        "author" to StringControl(label = "Author"),
        "year" to IntegerControl(label = "Year"),
        "status" to EnumControl<ReadingStatus>(label = "Status"),
        "notes" to StringControl(label = "Notes", multiline = true),

        "save" to ButtonControl(label = "Save", type = ButtonType.Primary),
        "cancel" to ButtonControl(label = "Cancel", type = ButtonType.Secondary)
    )

    override fun defineContentOrder() = listOf("title", "author", "year", "status", "notes")

    override fun defineActionBarOrder() = listOf("cancel", "save")
}
```

### 2. Implement Data Manager

```kotlin
class BookFormDataManager : FormDataManager() {

    override suspend fun loadData(entityId: Int?): Map<String, Any?> {
        if (entityId == null) return emptyMap()

        return dataAccess.select("title", "author", "year", "status", "notes")
            .from("books")
            .where("id = :id")
            .toObject<BookFormData>("id" to entityId)
            .toMap()
    }

    override suspend fun saveData(data: FormResultData): Int {
        // data.changes contains only modified fields
        // data.all contains all form values
        return dataAccess.insertInto("books")
            .values(data.all)
            .returning("id")
            .toField<Int>()
    }
}
```

### 3. Render the Form

```kotlin
@Composable
fun BookFormScreen(bookId: Int?) {
    FormScreen(
        schemaBuilder = BookFormSchemaBuilder(),
        dataManager = BookFormDataManager(),
        validator = BookFormValidator(),
        entityId = bookId
    )
}
```

## Dependencies

Control visibility and requirements based on other control values:

```kotlin
// Show "publisher" only when status is "Published"
"publisher" to StringControl(
    label = "Publisher",
    dependencies = mapOf(
        "visibility" to ControlDependency(
            controlName = "status",
            value = BookStatus.Published,
            dependencyType = DependencyType.Visible,
            comparisonType = ComparisonType.Equals
        )
    )
)

// Make "isbn" required only when "hasIsbn" is checked
"isbn" to StringControl(
    label = "ISBN",
    dependencies = mapOf(
        "required" to ControlDependency(
            controlName = "hasIsbn",
            value = true,
            dependencyType = DependencyType.Required,
            comparisonType = ComparisonType.Equals
        )
    )
)
```

## Repeatable Sections

Dynamic rows for collections like order items or authors:

```kotlin
"authors" to RepeatableControl(
    label = "Authors",
    minRows = 1,
    maxRows = 10,
    rowControls = mapOf(
        "name" to StringControl(label = "Name", required = true),
        "role" to EnumControl<AuthorRole>(label = "Role")
    )
)
```

## Validation

```kotlin
class BookFormValidator : FormValidator() {

    override fun defineRules(): List<ValidationRule> = listOf(
        ValidationRule(
            field = "year",
            condition = { data ->
                val year = data["year"] as? Int ?: return@ValidationRule true
                year in 1450..LocalDate.now().year
            },
            message = "Year must be between 1450 and current year"
        ),
        ValidationRule(
            field = "isbn",
            condition = { data ->
                val isbn = data["isbn"] as? String ?: return@ValidationRule true
                isbn.matches(Regex("^\\d{10}|\\d{13}$"))
            },
            message = "ISBN must be 10 or 13 digits"
        )
    )
}
```

## Architecture

```
form-engine/
├── component/
│   ├── FormSchema.kt          # Form structure definition
│   ├── FormState.kt           # Runtime state management
│   ├── FormHandler.kt         # Orchestrates form lifecycle
│   ├── FormScreen.kt          # Main Compose UI
│   ├── FormDataManager.kt     # Data loading/saving interface
│   └── FormValidator.kt       # Validation rules interface
│
├── control/
│   ├── base/
│   │   ├── Control.kt         # Base control class
│   │   ├── Dependencies.kt    # Dependency system
│   │   └── ControlAction.kt   # Action triggers
│   ├── type/                  # Control implementations
│   └── validator/             # Control-specific validators
│
└── layout/                    # Rendering utilities
```
