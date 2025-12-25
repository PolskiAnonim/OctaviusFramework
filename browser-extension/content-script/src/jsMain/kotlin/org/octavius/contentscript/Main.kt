package org.octavius.contentscript

import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.octavius.api.contract.Parser
import org.octavius.extension.util.chrome
import org.octavius.modules.asian.MangaUpdatesParser

@OptIn(DelicateCoroutinesApi::class)
fun main() {
    println("Octavius Content Script (z logiką) załadowany!")

    val availableParsers: List<Parser> = listOf(
        MangaUpdatesParser
    )

    chrome.runtime.onMessage.addListener { message, _, sendResponse ->
        if (message.action == "parsePage") {
            GlobalScope.launch { // Parsowanie może być asynchroniczne
                val url = window.location.href
                val parser = availableParsers.firstOrNull { it.canParse(url) }

                if (parser != null) {
                    val parsedData = parser.parse()
                    if (parsedData != null) {
                        // Serializujemy do JSON i wysyłamy!
                        val jsonResponse = Json.encodeToString(parsedData.serializer(), parsedData)
                        sendResponse(js("{ success: true, data: jsonResponse }"))
                    } else {
                        sendResponse(js("{ success: false, error: 'Parsing failed' }"))
                    }
                } else {
                    sendResponse(js("{ success: false, error: 'No suitable parser found' }"))
                }
            }
        }
        true // Kluczowe dla asynchronicznej odpowiedzi!
    }
}