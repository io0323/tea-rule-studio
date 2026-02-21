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
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.dto.ImportTeaLotsRequest
import studio.tearule.api.dto.ImportTeaLotsResponse
import studio.tearule.repository.TeaLotRepository
import studio.tearule.api.validation.ValidationUtils
import studio.tearule.api.validation.ValidationResult

fun Route.teaLotRoutes(teaLotRepository: TeaLotRepository) {
    route("/tea-lots") {
        get {
            val teaLots = teaLotRepository.findAll()
            call.respond(teaLots)
        }

        post {
            val request = call.receive<CreateTeaLotRequest>()
            val validationResult = ValidationUtils.validateCreateTeaLotRequest(request)
            if (validationResult is ValidationUtils.ValidationResult.Invalid) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Validation failed",
                    "details" to validationResult.errors
                ))
            }
            val teaLot = teaLotRepository.create(request)
            call.respond(HttpStatusCode.Created, teaLot)
        }

        delete {
            val ids = call.receive<List<Long>>()
            val count = teaLotRepository.deleteByIds(ids)
            call.respond(mapOf("deleted" to count))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

            val teaLot = teaLotRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "tea lot not found"))

            call.respond(teaLot)
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
        }
    }

    get("/export/tea-lots") {
        val teaLots = teaLotRepository.findAll()
        call.response.headers.append("Content-Disposition", "attachment; filename=\"tea-lots.json\"")
        call.response.headers.append("Content-Type", "application/json")
        call.respond(teaLots)
    }

    post("/import/tea-lots") {
        val request = call.receive<ImportTeaLotsRequest>()
        val validationResult = ValidationUtils.validateImportTeaLotsRequest(request)
        if (validationResult is ValidationUtils.ValidationResult.Invalid) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf(
                "error" to "Validation failed",
                "details" to validationResult.errors
            ))
        }
        val importedTeaLots = request.teaLots.map { teaLotRepository.create(it) }
        val response = ImportTeaLotsResponse(importedTeaLots.size, importedTeaLots)
        call.respond(HttpStatusCode.Created, response)
    }
}
