package org.octavius.feature.books.form.author.ui

import org.octavius.feature.books.form.author.BookAuthorDataManager
import org.octavius.feature.books.form.author.BookAuthorSchemaBuilder
import org.octavius.feature.books.form.author.BookAuthorValidator
import org.octavius.form.component.FormHandler
import org.octavius.form.component.FormScreen
import org.octavius.localization.Tr

class BookAuthorFormScreen {
    companion object {
        fun create(
            entityId: Int? = null
        ): FormScreen {
            val title =
                if (entityId == null) Tr.Books.Authors.Form.newAuthor() else Tr.Books.Authors.Form.editAuthor()

            return FormScreen(
                title,
                FormHandler(
                    entityId,
                    BookAuthorSchemaBuilder(),
                    BookAuthorDataManager(),
                    BookAuthorValidator(entityId)
                )
            )
        }
    }
}