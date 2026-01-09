package org.octavius.ui.draganddrop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.awtTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

// Definiujemy unikalny typ danych dla naszej aplikacji (lokalna referencja)
val LocalObjectFlavor = DataFlavor(
    DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Object",
    "Local Object Reference"
)

class LocalTransferable(private val obj: Any) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(LocalObjectFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return flavor == LocalObjectFlavor
    }

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) {
            throw UnsupportedFlavorException(flavor)
        }
        return obj
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("UNCHECKED_CAST")
fun <T> DragAndDropEvent.getLocalData(): T? {
    return try {
        val transferable = this.awtTransferable
        if (transferable.isDataFlavorSupported(LocalObjectFlavor)) {
            transferable.getTransferData(LocalObjectFlavor) as? T
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}