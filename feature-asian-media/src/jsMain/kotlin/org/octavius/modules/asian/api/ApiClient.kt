package org.octavius.modules.asian.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.octavius.modules.asian.model.PublicationAddRequest
import org.octavius.modules.asian.model.PublicationAddResponse

object ApiClient {

    private const val BASE_URL = "http://localhost:8080"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun addPublication(request: PublicationAddRequest): PublicationAddResponse {
        return try {
            client.post("$BASE_URL/api/asian-media/add") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (e: Exception) {
            println("Błąd API: ${e.message}")
            PublicationAddResponse(
                success = false,
                message = "Nie można połączyć się z serwerem Octavius. Upewnij się, że aplikacja jest uruchomiona."
            )
        }
    }
}