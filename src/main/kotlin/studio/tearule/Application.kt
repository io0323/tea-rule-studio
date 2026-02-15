package studio.tearule

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import studio.tearule.api.routes.ruleRoutes
import studio.tearule.api.routes.simulationRoutes
import studio.tearule.api.routes.teaLotRoutes
import studio.tearule.db.DatabaseFactory
import studio.tearule.repository.RuleRepository
import studio.tearule.repository.TeaLotRepository
import studio.tearule.seed.InitialData
import studio.tearule.service.RuleEvaluationService

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    DatabaseFactory.init(environment.config)
    InitialData.seedIfEmpty()

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                isLenient = false
                ignoreUnknownKeys = false
                explicitNulls = false
            },
        )
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                status = io.ktor.http.HttpStatusCode.BadRequest,
                message = mapOf(
                    "message" to (cause.message ?: "Invalid request"),
                    "status" to 400,
                ),
            )
        }
        exception<Exception> { call, cause ->
            call.respond(
                status = io.ktor.http.HttpStatusCode.InternalServerError,
                message = mapOf(
                    "message" to (cause.message ?: "Internal server error"),
                    "status" to 500,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                status = io.ktor.http.HttpStatusCode.InternalServerError,
                message = mapOf(
                    "message" to "Unexpected error",
                    "status" to 500,
                ),
            )
        }
    }

    routing {
        val ruleRepository = RuleRepository()
        val teaLotRepository = TeaLotRepository()
        val ruleEvaluationService = RuleEvaluationService(ruleRepository, teaLotRepository)

        staticResources("/static", "static")
        get("/") {
            call.respondRedirect("/static/index.html")
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        ruleRoutes(ruleRepository)
        teaLotRoutes(teaLotRepository)
        simulationRoutes(ruleEvaluationService)
    }
}
