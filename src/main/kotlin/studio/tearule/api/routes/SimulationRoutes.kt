package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import studio.tearule.api.dto.BulkSimulationRequest
import studio.tearule.api.dto.BulkSimulationResponse
import studio.tearule.service.RuleEvaluationService
import studio.tearule.api.validation.ValidationUtils

fun Route.simulationRoutes(ruleEvaluationService: RuleEvaluationService) {
    route("/simulate") {
        post("/{teaLotId}") {
            val teaLotId = call.parameters["teaLotId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid teaLotId"))

            val response = try {
                ruleEvaluationService.simulate(teaLotId)
            } catch (e: IllegalStateException) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to (e.message ?: "not found")))
            }

            call.respond(response)
        }

        post {
            val request = call.receive<BulkSimulationRequest>()
            val validationResult = ValidationUtils.validateBulkSimulationRequest(request)
            if (validationResult is ValidationUtils.ValidationResult.Invalid) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Validation failed",
                    "details" to validationResult.errors
                ))
            }
            val results = request.teaLotIds.map { teaLotId ->
                try {
                    ruleEvaluationService.simulate(teaLotId)
                } catch (e: IllegalStateException) {
                    null
                }
            }.filterNotNull()
            
            call.respond(BulkSimulationResponse(results))
        }
    }
}
