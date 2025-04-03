package org.octavius.novels.screens

import androidx.compose.runtime.Composable
import org.octavius.novels.form.NovelForm
import org.octavius.novels.navigator.Screen

class NovelEditScreen(novelId: Int?): Screen {

    private val novelForm: NovelForm = NovelForm(novelId)

    @Composable
    override fun Content() {
        novelForm.display()
    }
}