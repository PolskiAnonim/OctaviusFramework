package org.octavius.draganddrop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor

@OptIn(ExperimentalComposeUiApi::class)
fun extractTransferData(event: DragAndDropEvent): String? {
    return event.awtTransferable.let { transferable ->
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            transferable.getTransferData(DataFlavor.stringFlavor) as String
        } else {
            null
        }
    }
}