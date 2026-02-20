package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.plugins.openapi.*
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.dto.ImportTeaLotsRequest
import studio.tearule.api.dto.ImportTeaLotsResponse
import studio.tearule.repository.TeaLotRepository

fun Route.teaLotRoutes(teaLotRepository: TeaLotRepository) {
    route("/tea-lots") {
        get {
            val teaLots = teaLotRepository.findAll()
            call.respond(teaLots)
        } openapi {
            summary = "Get all tea lots"
            description = "Retrieve a list of all tea lots"
            operationId = "getAllTeaLots"
            response {
                HttpStatusCode.OK to {
                    description("List of tea lots")
                    body<List<studio.tearule.api.dto.TeaLotResponse>>()
                }
            }
        }

        post {
            val request = call.receive<CreateTeaLotRequest>()
            val teaLot = teaLotRepository.create(request)
            call.respond(HttpStatusCode.Created, teaLot)
        } openapi {
            summary = "Create a new tea lot"
            description = "Create a new tea lot with data validation"
            operationId = "createTeaLot"
            requestBody {
                description("Tea lot creation request")
                body<CreateTeaLotRequest>()
            }
            response {
                HttpStatusCode.Created to {
                    description("Created tea lot")
                    body<studio.tearule.api.dto.TeaLotResponse>()
                }
                HttpStatusCode.BadRequest to {
                    description("Invalid request data or validation errors")
                }
            }
        }

        delete {
            val ids = call.receive<List<Long>>()
            val count = teaLotRepository.deleteByIds(ids)
            call.respond(mapOf("deleted" to count))
        } openapi {
            summary = "Delete multiple tea lots"
            description = "Delete multiple tea lots by their IDs"
            operationId = "deleteMultipleTeaLots"
            requestBody {
                description("List of tea lot IDs to delete")
                body<List<Long>>()
            }
            response {
                HttpStatusCode.OK to {
                    description("Number of deleted tea lots")
                    body<Map<String, Int>>()
                }
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

            val teaLot = teaLotRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "tea lot not found"))

            call.respond(teaLot)
        } openapi {
            summary = "Get tea lot by ID"
            description = "Retrieve a specific tea lot by its ID"
            operationId = "getTeaLotById"
            parameters {
                pathParameter<Long>("id") {
                    description("Tea lot ID")
                }
            }
            response {
                HttpStatusCode.OK to {
                    description("Tea lot details")
                    body<studio.tearule.api.dto.TeaLotResponse>()
                }
                HttpStatusCode.NotFound to {
                    description("Tea lot not found")
                }
                HttpStatusCode.BadRequest to {
                    description("Invalid ID format")
                }
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

            val deleted = teaLotRepository.deleteById(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "tea lot not found"))
            }
        } openapi {
            summary = "Delete tea lot by ID"
            description = "Delete a specific tea lot by its ID"
            operationId = "deleteTeaLotById"
            parameters {
                pathParameter<Long>("id") {
                    description("Tea lot ID")
                }
            }
            response {
                HttpStatusCode.NoContent to {
                    description("Tea lot deleted successfully")
                }
                HttpStatusCode.NotFound to {
                    description("Tea lot not found")
                }
                HttpStatusCode.BadRequest to {
                    description("Invalid ID format")
                }
            }
        }
    }

    get("/export/tea-lots") {
        val teaLots = teaLotRepository.findAll()
        call.response.headers.append("Content-Disposition", "attachment; filename=\"tea-lots.json\"")
        call.response.headers.append("Content-Type", "application/json")
        call.respond(teaLots)
    } openapi {
        summary = "Export all tea lots"
        description = "Download all tea lots as a JSON file"
        operationId = "exportTeaLots"
        response {
            HttpStatusCode.OK to { r: OpenApiResponse ->
                r.description = "JSON file containing all tea lots"
                r.body<List<studio.tearule.api.dto.TeaLotResponse>>()
            }
        }
    }

    post("/import/tea-lots") {
        val request = call.receive<ImportTeaLotsRequest>()
        val importedTeaLots = request.teaLots.map { teaLotRepository.create(it) }
        val response = ImportTeaLotsResponse(importedTeaLots.size, importedTeaLots)
        call.respond(HttpStatusCode.Created, response)
    }
}
