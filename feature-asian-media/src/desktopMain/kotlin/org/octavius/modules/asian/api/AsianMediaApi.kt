package org.octavius.modules.asian.api

import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.api.contract.ApiModule
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toField
import org.octavius.data.builder.toSingle
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.type.PgStandardType
import org.octavius.data.type.withPgType
import org.octavius.domain.asian.PublicationStatus
import org.octavius.modules.asian.AsianMediaFeature
import org.octavius.modules.asian.model.PublicationAddRequest
import org.octavius.modules.asian.model.PublicationAddResponse
import org.octavius.modules.asian.model.PublicationCheckRequest
import org.octavius.modules.asian.model.PublicationCheckResponse
import org.octavius.navigation.NavigationEvent
import org.octavius.navigation.NavigationEventBus

/**
 * Implementacja ApiModule dla funkcjonalności "Asian Media".
 * Definiuje endpointy do sprawdzania i dodawania publikacji.
 * Używa Koin do wstrzykiwania zależności (DataAccess, BatchExecutor).
 */
class AsianMediaApi : ApiModule, KoinComponent {
    private val dataAccess: DataAccess by inject()

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
            if (request.titles.isEmpty()) {
                call.respond(PublicationCheckResponse(found = false))
                return@post
            }

            val result = dataAccess.select("id", "titles")
                .from("asian_media.titles")
                .where("titles && :titles")
                .toSingle("titles" to request.titles.withPgType(PgStandardType.TEXT_ARRAY))

            when (result) {
                is DataResult.Failure -> {
                    call.respond(PublicationCheckResponse(found = false)) // TODO Error
                }
                is DataResult.Success -> {
                    val row = result.value
                    if (row != null) {
                        val titleId = row["id"] as Int

                        @Suppress("UNCHECKED_CAST")
                        val dbTitles = row["titles"] as List<String>

                        val matchedTitle = request.titles.firstOrNull { it in dbTitles }

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

            val plan = TransactionPlan()

            // Krok 1: Wstaw tytuł i uzyskaj bezpieczny uchwyt do jego przyszłego ID
            val titleData = mapOf(
                "titles" to request.titles.withPgType(PgStandardType.TEXT_ARRAY),
                "language" to request.language
            )
            val titleIdHandle = plan.add(
                dataAccess.insertInto("asian_media.titles")
                    .values(titleData)
                    .returning("id")
                    .asStep()
                    .toField<Int>(titleData)
            )

            // Krok 2: Wstaw publikację, używając referencji do ID z kroku 1
            val publicationData = mapOf(
                "publication_type" to request.type,
                "status" to PublicationStatus.NotReading,
                "track_progress" to false,
                "title_id" to titleIdHandle.field()
            )
            plan.add(
                dataAccess.insertInto("asian_media.publications")
                    .values(publicationData)
                    .asStep()
                    .execute(publicationData)
            )

            // Wykonanie planu
            val result = dataAccess.executeTransactionPlan(plan)

            when (result) {
                is DataResult.Failure -> {
                    call.respond(
                        PublicationAddResponse(
                            success = false,
                            message = "Wystąpił błąd: ${result.error.message}"
                        )
                    )
                }
                is DataResult.Success -> {
                    // Pobierz wynik z pierwszego kroku, używając bezpiecznego uchwytu
                    val newId = result.value.get(titleIdHandle)

                    if (newId != null) {
                        println("API: Pomyślnie dodano tytuł z ID: $newId. Wysyłanie zdarzenia nawigacyjnego...")

                        val payload = mapOf("entityId" to newId)
                        NavigationEventBus.post(
                            NavigationEvent.Navigate(
                                screenId = AsianMediaFeature.ASIAN_MEDIA_FORM_SCREEN_ID,
                                payload = payload,
                                tabId = "asian_media"
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