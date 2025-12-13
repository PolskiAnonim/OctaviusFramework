# Form Engine

<div align="center">

**A declarative, data-driven form framework for Compose Multiplatform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![KDoc](https://img.shields.io/badge/KDoc-Documentation-blue)](https://polskianonim.github.io/OctaviusFramework/form-engine/)

</div>

---

## Overview

Form Engine is a framework for building complex, interactive forms in Compose. Define your form structure declaratively with a schema, and the engine handles rendering, validation, state management, and data binding automatically.

## Features

- **Declarative Schema** — Define forms as data, not UI code
- **Rich Control Types** — Primitives, dropdowns, sections, repeatable rows
- **Dependencies** — Show/hide or require controls based on other values
- **Actions** — Trigger logic on value changes or button clicks
- **Validation** — Field-level and business rule validation
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

        "save" to ButtonControl(
            text = "Save",
            buttonType = ButtonType.Filled,
            actions = listOf(ControlAction { trigger.triggerAction("save", validates = true) })
        ),
        "cancel" to ButtonControl(
            text = "Cancel",
            buttonType = ButtonType.Outlined,
            actions = listOf(ControlAction { trigger.triggerAction("cancel", validates = false) })
        )
    )

    override fun defineContentOrder() = listOf("title", "author", "year", "status")

    override fun defineActionBarOrder() = listOf("cancel", "save")
}
```

### 2. Implement Data Manager

```kotlin
class BookFormDataManager : FormDataManager() {

    override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
        return loadData(loadedId) {
            from("books", "b")
            map("title")
            map("author")
            map("year")
            map("status")
        }
    }

    override fun definedFormActions() = mapOf(
        "save" to { formData, loadedId ->
            val result = if (loadedId == null) {
                dataAccess.insertInto("books")
                    .values(formData.all)
                    .execute()
            } else {
                dataAccess.update("books")
                    .setValues(formData.changes)
                    .where("id = :id")
                    .execute("id" to loadedId)
            }
            if (result is DataResult.Success) FormActionResult.CloseScreen
            else FormActionResult.Failure
        },
        "cancel" to { _, _ -> FormActionResult.CloseScreen }
    )
}
```

### 3. Render the Form

```kotlin
@Composable
fun BookFormScreen(bookId: Int?) {
    FormScreen(
        title = "Books",
        FormHandler(
            entityId = bookId,
            formSchemaBuilder = BookFormSchemaBuilder(),
            formDataManager = BookFormDataManager(),
            formValidator = BookFormValidator(),
        )
    )
}
```

## Data Loading DSL

The `loadData` function provides a DSL for loading form data:

```kotlin
override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
    return loadData(loadedId) {
        // Main table
        from("books", "b")
        map("title", "title") // control name -> db column (dafault is control name in snake case)
        map("author")
        map("publicationYear", "pub_year")

        // One-to-one relation
        mapOneToOne {
            from("book_details", "bd")
            on("bd.book_id = b.id")
            map("isbn")
            map("pageCount", "page_count")
            existenceFlag("hasDetails", "bd.id")  // boolean flag if relation exists
        }

        // One-to-many relation (for RepeatableControl)
        mapRelatedList("authors") {
            from("book_authors", "ba")
            linkedBy("ba.book_id")
            map("name")
            map("role")
        }
    }
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

    override fun validateBusinessRules(formResultData: FormResultData): Boolean {
        val year = formResultData.all["year"] as? Int

        if (year != null && year > LocalDate.now().year) {
            errorManager.addFieldError("year", "Year cannot be in the future")
            return false
        }

        return true
    }

    override fun defineActionValidations() = mapOf(
        "publish" to { formData ->
            val isbn = formData.all["isbn"] as? String
            if (isbn.isNullOrBlank()) {
                errorManager.addFieldError("isbn", "ISBN is required for publishing")
                false
            } else true
        }
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
│   ├── FormDataManager.kt     # Data loading/saving
│   ├── FormLoader.kt          # Data loading DSL
│   └── FormValidator.kt       # Validation logic
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
