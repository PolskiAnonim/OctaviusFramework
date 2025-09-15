package org.octavius.api.server

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.octavius.api.contract.ApiModule

class EmbeddedServer(private val apiModules: List<ApiModule>) {

    val server = embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) {
            json()
        }

        install(CORS) {

            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)

            // Przeglądarki często wysyłają zapytanie OPTIONS przed właściwym żądaniem (preflight)
            allowMethod(HttpMethod.Options)

            // Pozwól na nagłówek Content-Type, który jest wysyłany przy POST z JSON-em
            allowHeader(HttpHeaders.ContentType)

            anyHost()
        }

        module(apiModules)
    }

    fun run() {
        server.start(wait = true)
    }
}

fun Application.module(apiModules: List<ApiModule>) {
    configureRouting(apiModules)
}

fun Application.configureRouting(apiModules: List<ApiModule>) {
    routing {
        // Podstawowy, powitalny endpoint
        get("/") {
            call.respondText("Hello from Octavius Server!")
        }

        // iterujemy po wszystkich modułach i prosimy je o instalację swoich ścieżek
        apiModules.forEach { module ->
            module.installRoutes(this)
        }
    }
}