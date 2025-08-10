package org.octavius.extension.util


// Definiujemy globalny obiekt `chrome`
external val chrome: Chrome

// Definiujemy strukturę API, której potrzebujemy
external interface Chrome {
    val runtime: Runtime
    val tabs: Tabs
}

external interface Runtime {
    val onMessage: OnMessage
}

external interface OnMessage {
    fun addListener(listener: (message: dynamic, sender: MessageSender, sendResponse: (dynamic) -> Unit) -> Boolean)
}

external interface MessageSender {
    val tab: Tab?
    val id: String?
}

external interface Tabs {
    fun query(queryInfo: QueryInfo, callback: (Array<Tab>) -> Unit)
    fun sendMessage(tabId: Int, message: dynamic, responseCallback: (dynamic) -> Unit)
}

external interface Tab {
    val id: Int?
    val url: String?
    val title: String?
}

// To jest klasa pomocnicza, a nie deklaracja `external`,
// bo tworzymy jej instancje w naszym kodzie.
data class QueryInfo(
    val active: Boolean,
    val currentWindow: Boolean
)