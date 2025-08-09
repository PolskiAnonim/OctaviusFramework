package org.octavius.modules.asian.api

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.api.contract.ApiModule
import org.octavius.api.contract.asian.PublicationAddRequest
import org.octavius.api.contract.asian.PublicationAddResponse
import org.octavius.api.contract.asian.PublicationCheckRequest
import org.octavius.api.contract.asian.PublicationCheckResponse
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.domain.asian.PublicationStatus

/**
 * Implementacja ApiModule dla funkcjonalności "Asian Media".
 * Definiuje endpointy do sprawdzania i dodawania publikacji.
 * Używa Koin do wstrzykiwania zależności (DataFetcher, BatchExecutor).
 */
class AsianMediaApi : ApiModule, KoinComponent {
    private val fetcher: DataFetcher by inject()
    private val batchExecutor: BatchExecutor by inject()

    override fun installRoutes(routing: Routing) {
        routing.route("/api/asian-media") {
            // Endpoint do sprawdzania istnienia tytułu
            checkPublicationExistence()

            // Endpoint do dodawania nowego tytułu
            addNewPublication()
        }
    }

    /**
     * Definiuje endpoint: GET /api/asian-media/check
     * Sprawdza, czy którykolwiek z podanych tytułów istnieje już w bazie.
     */
    private fun Route.checkPublicationExistence() {
        post("/check") {
            val request = call.receive<PublicationCheckRequest>()
            val titles = request.titles
            if (titles.isEmpty()) {
                call.respond(PublicationCheckResponse(found = false))
                return@post
            }

            // Używamy operatora && (overlap) z PostgreSQL do sprawdzenia,
            // czy tablica tytułów w bazie ma jakikolwiek wspólny element z listą z zapytania.
            val result = fetcher.fetchRowOrNull(
                table = "titles",
                columns = "id, titles",
                filter = "titles && :titles::text[]", // TODO lepsze rozwiązanie rzutowania na text[]
                params = mapOf("titles" to titles)
            )

            if (result != null) {
                val titleId = result["id"] as Int
                @Suppress("UNCHECKED_CAST")
                val dbTitles = result["titles"] as List<String>
                // Znajdź, który konkretnie tytuł pasował
                val matchedTitle = titles.firstOrNull { it in dbTitles }

                call.respond(PublicationCheckResponse(
                    found = true,
                    titleId = titleId,
                    matchedTitle = matchedTitle
                ))
            } else {
                call.respond(PublicationCheckResponse(found = false))
            }
        }
    }

    /**
     * Definiuje endpoint: POST /api/asian-media/add
     * Dodaje nowy tytuł i powiązaną z nim publikację do bazy danych.
     */
    private fun Route.addNewPublication() {
        post("/add") {
            try {
                val request = call.receive<PublicationAddRequest>()

                // Przygotuj operacje bazodanowe w jednej transakcji
                val newTitle = mapOf(
                    "titles" to DatabaseValue.Value(request.titles),
                    "language" to DatabaseValue.Value(request.language)
                )
                // Domyślnie dodajemy publikację ze statusem NotReading
                val newPublication = mapOf(
                    "publication_type" to DatabaseValue.Value(request.type),
                    "status" to DatabaseValue.Value(PublicationStatus.NotReading),
                    "track_progress" to DatabaseValue.Value(false)
                )

                val steps = listOf(
                    // Krok 0: Wstaw nowy tytuł i zwróć jego ID
                    DatabaseStep.Insert("titles", newTitle, returning = listOf("id")),
                    // Krok 1: Wstaw nową publikację, używając ID z kroku 0
                    DatabaseStep.Insert("publications", newPublication +
                            mapOf("title_id" to DatabaseValue.FromStep(0, "id"))
                    )
                )

                val result = batchExecutor.execute(steps)
                val newId = result[0]?.first()?.get("id") as? Int

                if (newId != null) {
                    call.respond(
                        PublicationAddResponse(
                            success = true,
                            newTitleId = newId,
                            message = "Pomyślnie dodano nowy tytuł do bazy."
                        )
                    )
                } else {
                    throw IllegalStateException("Nie udało się uzyskać ID nowo wstawionego tytułu.")
                }

            } catch (e: Exception) {
                // Obsłuż błędy, np. błąd deserializacji, błąd bazy danych
                call.respond(PublicationAddResponse(
                    success = false,
                    message = "Wystąpił błąd: ${e.message}"
                ))
            }
        }
    }
}