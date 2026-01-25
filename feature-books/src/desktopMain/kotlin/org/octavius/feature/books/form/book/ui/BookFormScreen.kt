package org.octavius.feature.books.form.book.ui

import org.octavius.feature.books.form.book.BookFormDataManager
import org.octavius.feature.books.form.book.BookFormSchemaBuilder
import org.octavius.feature.books.form.book.BookFormValidator
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr

class BookFormScreen {
    companion object {
        fun create(
            entityId: Int? = null
        ): FormScreen {
            val title =
                if (entityId == null) Tr.Books.Form.newBook() else Tr.Books.Form.editBook()

            return FormScreen(
                title,
                FormHandler(
                    entityId,
                    BookFormSchemaBuilder(),
                    BookFormDataManager(),
                    BookFormValidator(entityId)
                )
            )
        }
    }
}