package org.octavius.report

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object CellRendererUtils {
    @Composable
    fun StandardCellWrapper(
        modifier: Modifier,
        alignment: Alignment = Alignment.CenterStart,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = alignment
        ) {
            content()
        }
    }
}