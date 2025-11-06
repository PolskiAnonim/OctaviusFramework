package org.octavius.util

import org.w3c.xhr.XMLHttpRequest


actual fun loadResources(name: String): List<String> {
    val request = XMLHttpRequest()
    request.open("GET", name, false)
    request.send(null)
    println(request.responseText)
    if (request.status.toInt() == 200) {
        return listOf(request.responseText)
    } else {
        println("ERROR: Synchronous XHR failed for resource '$name'. Status: ${request.status}")
        return emptyList()
    }
}