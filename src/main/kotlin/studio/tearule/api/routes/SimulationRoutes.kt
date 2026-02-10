package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
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
        }
    }
}
