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
        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid rule ID")
                return@delete
            }
            val deleted = ruleRepository.deleteById(id)
            if (deleted) {
                call.respond(HttpStatusCode.OK, mapOf("deleted" to true))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("deleted" to false))
            }
        }
        delete {
            val ids = call.receive<List<Long>>()
            val count = ruleRepository.deleteByIds(ids)
            call.respond(HttpStatusCode.OK, mapOf("deletedCount" to count))
        }
    }
}
