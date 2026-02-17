package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.plugins.openapi.*
import studio.tearule.api.dto.BulkSimulationRequest
import studio.tearule.api.dto.BulkSimulationResponse
import studio.tearule.service.RuleEvaluationService

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
        } openapi {
            summary = "Simulate rules for a single tea lot"
            description = "Run all rules against a specific tea lot and return evaluation results"
            operationId = "simulateTeaLot"
            parameters {
                pathParameter<Long>("teaLotId") {
                    description = "ID of the tea lot to simulate"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Simulation results"
                    body<studio.tearule.api.dto.SimulationResponse>()
                }
                HttpStatusCode.NotFound to {
                    description = "Tea lot not found or no rules available"
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid tea lot ID"
                }
            }
        }

        post {
            val request = call.receive<BulkSimulationRequest>()
            val results = request.teaLotIds.map { teaLotId ->
                try {
                    ruleEvaluationService.simulate(teaLotId)
                } catch (e: IllegalStateException) {
                    null
                }
            }.filterNotNull()
            
            call.respond(BulkSimulationResponse(results))
        } openapi {
            summary = "Simulate rules for multiple tea lots"
            description = "Run all rules against multiple tea lots and return evaluation results for each"
            operationId = "bulkSimulateTeaLots"
            requestBody {
                description = "List of tea lot IDs to simulate"
                body<BulkSimulationRequest>()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Bulk simulation results"
                    body<BulkSimulationResponse>()
                }
            }
        }
    }
}
