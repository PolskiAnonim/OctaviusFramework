package org.octavius.contentscript

import org.octavius.contentscript.dom.MangaUpdatesParser
import org.octavius.extension.util.chrome

fun main() {
    println("Octavius Content Script załadowany!")

    chrome.runtime.onMessage.addListener { message, _, sendResponse ->
        if (message.action == "parsePage") {
            val pageData = MangaUpdatesParser.parsePage()

            // Konwertujemy na dynamiczny obiekt JS do wysłania
            val response = js("({})")
            response.titles = pageData.titles.toTypedArray()
            response.type = pageData.type.name
            response.language = pageData.language.name

            sendResponse(response)
        }
        true
    }
}