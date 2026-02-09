package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.repository.RuleRepository

fun Route.ruleRoutes(ruleRepository: RuleRepository) {
    route("/rules") {
        get {
            call.respond(ruleRepository.findAll())
        }
        post {
            val request = call.receive<CreateRuleRequest>()
            val created = ruleRepository.create(request)
            call.respond(HttpStatusCode.Created, created)
        }
    }
}
