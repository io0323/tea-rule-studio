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
import studio.tearule.api.validation.ValidationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import studio.tearule.api.dto.ApiResponse

fun Route.simulationRoutes(ruleEvaluationService: RuleEvaluationService) {
    val logger = LoggerFactory.getLogger("SimulationRoutes")
    route("/simulate") {
        post("/{teaLotId}") {
            val teaLotId = call.parameters["teaLotId"]?.toLongOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<String>(success = false, error = "invalid teaLotId"))

            val response = try {
                ruleEvaluationService.simulate(teaLotId)
            } catch (e: IllegalStateException) {
                return@post call.respond(HttpStatusCode.NotFound, ApiResponse<String>(success = false, error = (e.message ?: "not found")))
            }

            call.respond(ApiResponse<studio.tearule.api.dto.SimulationResponse>(success = true, data = response))
        }

        post {
            val request = call.receive<BulkSimulationRequest>()
            val validationResult = ValidationUtils.validateBulkSimulationRequest(request)
            if (validationResult is ValidationResult.Invalid) {
                logger.warn("Bulk simulation validation failed: {}", validationResult.errors.joinToString(", "))
                return@post call.respond(HttpStatusCode.BadRequest, ApiResponse<List<String>>(success = false, error = "Validation failed", data = validationResult.errors))
            }
            val results = request.teaLotIds.map { teaLotId ->
                try {
                    ruleEvaluationService.simulate(teaLotId)
                } catch (e: IllegalStateException) {
                    null
                }
            }.filterNotNull()
            
            call.respond(ApiResponse<studio.tearule.api.dto.BulkSimulationResponse>(success = true, data = BulkSimulationResponse(results)))
        }
    }
}
