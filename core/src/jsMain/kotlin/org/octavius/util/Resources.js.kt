package org.octavius.util

import org.w3c.xhr.XMLHttpRequest


actual fun loadResource(name: String): String? {
    val request = XMLHttpRequest()
    request.open("GET", name, false)
    request.send(null)
    println(request.responseText)
    return if (request.status.toInt() == 200) {
        request.responseText
    } else {
        println("ERROR: Synchronous XHR failed for resource '$name'. Status: ${request.status}")
        null
    }
}