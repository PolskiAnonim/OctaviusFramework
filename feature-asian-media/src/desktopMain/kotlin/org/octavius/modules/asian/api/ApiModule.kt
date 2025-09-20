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
import org.octavius.data.contract.*
import org.octavius.domain.asian.PublicationStatus
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.navigation.NavigationEvent
import org.octavius.navigation.NavigationEventBus

/**
 * Implementacja ApiModule dla funkcjonalności "Asian Media".
 * Definiuje endpointy do sprawdzania i dodawania publikacji.
 * Używa Koin do wstrzykiwania zależności (DataAccess, BatchExecutor).
 */
class AsianMediaApi : ApiModule, KoinComponent {
    private val fetcher: DataAccess by inject()
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
            val result = fetcher.select("id, titles").from("titles").where("titles && :titles")
                .toSingle(mapOf("titles" to titles.withPgType("text[]")))


            when (result) {
                is DataResult.Failure -> call.respond(PublicationCheckResponse(found = false)) // TODO error
                is DataResult.Success<Map<String, Any?>?> -> {
                    val value = result.value
                    if (value != null) {
                        val titleId = value["id"] as Int

                        @Suppress("UNCHECKED_CAST")
                        val dbTitles = value["titles"] as List<String>
                        // Znajdź, który konkretnie tytuł pasował
                        val matchedTitle = titles.firstOrNull { it in dbTitles }

                        call.respond(
                            PublicationCheckResponse(
                                found = true,
                                titleId = titleId,
                                matchedTitle = matchedTitle
                            )
                        )
                    } else {
                        call.respond(PublicationCheckResponse(found = false))
                    }
                }
            }


        }
    }

    /**
     * Definiuje endpoint: POST /api/asian-media/add
     * Dodaje nowy tytuł i powiązaną z nim publikację do bazy danych.
     */
    private fun Route.addNewPublication() {
        post("/add") {

            val request = call.receive<PublicationAddRequest>()

            // Przygotuj operacje bazodanowe w jednej transakcji
            val newTitle = mapOf(
                "titles" to request.titles.toDatabaseValue(),
                "language" to request.language.toDatabaseValue()
            )
            // Domyślnie dodajemy publikację ze statusem NotReading
            val newPublication = mapOf(
                "publication_type" to request.type.toDatabaseValue(),
                "status" to PublicationStatus.NotReading.toDatabaseValue(),
                "track_progress" to false.toDatabaseValue()
            )

            val steps = listOf(
                // Krok 0: Wstaw nowy tytuł i zwróć jego ID
                TransactionStep.Insert("titles", newTitle, returning = listOf("id")),
                // Krok 1: Wstaw nową publikację, używając ID z kroku 0
                TransactionStep.Insert(
                    "publications", newPublication +
                            mapOf("title_id" to DatabaseValue.FromStep(0, "id"))
                )
            )

            val result = batchExecutor.execute(steps)

            when (result) {
                is DataResult.Failure -> {
                    call.respond(
                        PublicationAddResponse(
                            success = false,
                            message = "Wystąpił błąd: ${result.error.message}"
                        )
                    )
                }

                is DataResult.Success<BatchStepResults> -> {
                    val newId = result.value[0]?.first()?.get("id") as? Int

                    if (newId != null) {
                        // === NOWA LOGIKA - WYSYŁANIE ZDARZENIA ===
                        println("API: Pomyślnie dodano tytuł z ID: $newId. Wysyłanie zdarzenia nawigacyjnego...")

                        // Przygotuj payload dla formularza
                        val payload = mapOf("entityId" to newId)

                        // Wyślij zdarzenie do magistrali
                        NavigationEventBus.post(
                            NavigationEvent.NavigateToScreen(
                                screenId = AsianMediaFeature.ASIAN_MEDIA_FORM_SCREEN_ID, // Użyjemy stałej
                                payload = payload
                            )
                        )

                        call.respond(
                            PublicationAddResponse(
                                success = true,
                                newTitleId = newId,
                                message = "Pomyślnie dodano nowy tytuł do bazy."
                            )
                        )
                    } else {
                        call.respond(
                            PublicationAddResponse(
                                success = false,
                                message = "Wystąpił błąd: Nie udało się uzyskać ID nowo wstawionego tytułu."
                            )
                        )
                    }
                }
            }
        }
    }
}