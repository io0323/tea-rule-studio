package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.repository.TeaLotRepository

fun Route.teaLotRoutes(teaLotRepository: TeaLotRepository) {
    route("/tea-lots") {
        get {
            call.respond(teaLotRepository.findAll())
        }

        get("/{teaLotId}") {
            val teaLotId = call.parameters["teaLotId"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid teaLotId"))

            val teaLot = teaLotRepository.findById(teaLotId)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "tea lot not found"))

            call.respond(teaLot)
        }

        post {
            val request = call.receive<CreateTeaLotRequest>()
            val created = teaLotRepository.create(request)
            call.respond(HttpStatusCode.Created, created)
        }
    }
}
