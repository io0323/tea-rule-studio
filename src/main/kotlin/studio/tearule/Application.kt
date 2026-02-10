package studio.tearule

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
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
        exception<Throwable> { call, cause ->
            call.respond(
                status = io.ktor.http.HttpStatusCode.InternalServerError,
                message = mapOf(
                    "error" to (cause.message ?: "unexpected error"),
                ),
            )
        }
    }

    routing {
        val ruleRepository = RuleRepository()
        val teaLotRepository = TeaLotRepository()
        val ruleEvaluationService = RuleEvaluationService(ruleRepository, teaLotRepository)

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        ruleRoutes(ruleRepository)
        teaLotRoutes(teaLotRepository)
        simulationRoutes(ruleEvaluationService)
    }
}
