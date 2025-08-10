package org.octavius.extension.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.octavius.api.contract.asian.PublicationAddRequest
import org.octavius.api.contract.asian.PublicationAddResponse

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