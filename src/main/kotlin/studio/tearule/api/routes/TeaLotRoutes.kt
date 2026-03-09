package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.http.HttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.method
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.dto.ImportTeaLotsRequest
import studio.tearule.api.dto.ImportTeaLotsResponse
import studio.tearule.api.dto.UpdateTeaLotRequest
import studio.tearule.repository.TeaLotRepository
import studio.tearule.api.validation.ValidationUtils
import studio.tearule.api.validation.ValidationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import studio.tearule.api.dto.ApiResponse

fun Route.teaLotRoutes(teaLotRepository: TeaLotRepository) {
    val logger = LoggerFactory.getLogger("TeaLotRoutes")
    route("/tea-lots") {
        get {
            val teaLots = teaLotRepository.findAll()
            call.respond(ApiResponse<List<studio.tearule.api.dto.TeaLotResponse>>(success = true, data = teaLots))
        }

        post {
            val request = call.receive<CreateTeaLotRequest>()
            val validationResult = ValidationUtils.validateCreateTeaLotRequest(request)
            if (validationResult is ValidationResult.Invalid) {
                logger.warn("Create tea lot validation failed: {}", validationResult.errors.joinToString(", "))
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<List<String>>(success = false, error = "Validation failed", data = validationResult.errors))
            }
            val teaLot = teaLotRepository.create(request)
            call.respond(HttpStatusCode.Created, ApiResponse<studio.tearule.api.dto.TeaLotResponse>(success = true, data = teaLot))
        }

        delete {
            val ids = call.receive<List<Long>>()
            val count = teaLotRepository.deleteByIds(ids)
            call.respond(ApiResponse<Map<String, Any>>(success = true, data = mapOf("deleted" to count)))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, error = "invalid id"))

            val teaLot = teaLotRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ApiResponse<String>(success = false, error = "tea lot not found"))

            call.respond(ApiResponse<studio.tearule.api.dto.TeaLotResponse>(success = true, data = teaLot))
        }
    }

    put("/tea-lots/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, error = "invalid id"))

        val request = call.receive<UpdateTeaLotRequest>()
        ValidationUtils.validateUpdateTeaLotRequest(request).let { result ->
            when (result) {
                is ValidationResult.Invalid -> {
                    logger.warn("Update tea lot validation failed: {}", result.errors.joinToString(", "))
                    return@put call.respond(HttpStatusCode.BadRequest, ApiResponse<List<String>>(success = false, error = "Validation failed", data = result.errors))
                }
                is ValidationResult.Valid -> {}
            }
        }

        val updatedTeaLot = teaLotRepository.update(id, request)
            ?: return@put call.respond(HttpStatusCode.NotFound, ApiResponse<String>(success = false, error = "tea lot not found"))

        call.respond(ApiResponse<studio.tearule.api.dto.TeaLotResponse>(success = true, data = updatedTeaLot))
    }

    delete("/tea-lots/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, error = "invalid id"))

        val deleted = teaLotRepository.deleteById(id)
        if (deleted) {
            call.respond(HttpStatusCode.NoContent, ApiResponse<Unit>(success = true))
        } else {
            call.respond(HttpStatusCode.NotFound, ApiResponse<String>(success = false, error = "tea lot not found"))
        }
    }

    get("/export/tea-lots") {
        val teaLots = teaLotRepository.findAll()
        call.response.headers.append("Content-Disposition", "attachment; filename=\"tea-lots.json\"")
        call.response.headers.append("Content-Type", "application/json")
        call.respond(ApiResponse<List<studio.tearule.api.dto.TeaLotResponse>>(success = true, data = teaLots))
    }

    post("/import/tea-lots") {
        val request = call.receive<ImportTeaLotsRequest>()
        val validationResult = ValidationUtils.validateImportTeaLotsRequest(request)
        if (validationResult is ValidationResult.Invalid) {
            logger.warn("Import tea lots validation failed: {}", validationResult.errors.joinToString(", "))
            return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<List<String>>(success = false, error = "Validation failed", data = validationResult.errors))
        }
        val importedTeaLots = request.teaLots.map { teaLotRepository.create(it) }
        val response = ImportTeaLotsResponse(importedTeaLots.size, importedTeaLots)
        call.respond(HttpStatusCode.Created, ApiResponse<studio.tearule.api.dto.ImportTeaLotsResponse>(success = true, data = response))
    }
}
