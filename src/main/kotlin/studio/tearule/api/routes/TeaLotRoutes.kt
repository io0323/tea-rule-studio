package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.repository.TeaLotRepository

fun Route.teaLotRoutes(teaLotRepository: TeaLotRepository) {
    route("/tea-lots") {
        post {
            val request = call.receive<CreateTeaLotRequest>()
            val created = teaLotRepository.create(request)
            call.respond(HttpStatusCode.Created, created)
        }
    }
}
