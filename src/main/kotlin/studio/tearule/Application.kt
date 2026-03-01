package studio.tearule

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.http.content.staticResources
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
import studio.tearule.middleware.RateLimitMiddleware

fun main(args: Array<String>) {
    // Set port from environment variable if available
    System.getenv("PORT")?.toIntOrNull()?.let { port ->
        System.setProperty("ktor.deployment.port", port.toString())
    }

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

    routing {
        val log = LoggerFactory.getLogger("Application")
        val ruleRepository = RuleRepository()
        val teaLotRepository = TeaLotRepository()
        val ruleEvaluationService = RuleEvaluationService(ruleRepository, teaLotRepository)
        val rateLimitMiddleware = RateLimitMiddleware()

        // Apply rate limiting to all routes
        intercept(io.ktor.server.application.ApplicationCallPipeline.Call) {
            rateLimitMiddleware.intercept(this)
        }

        staticResources("/static", "static")
        get("/") {
            call.respondRedirect("/static/index.html")
        }

        get("/health") {
            val dbConnected = DatabaseFactory.checkConnection()
            if (dbConnected) {
                log.info("Health check: Database connected")
            } else {
                log.error("Health check: Database disconnected")
            }
            call.respond(mapOf("status" to if (dbConnected) "ok" else "error", "database" to if (dbConnected) "connected" else "disconnected"))
        }

        ruleRoutes(ruleRepository)
        teaLotRoutes(teaLotRepository)
        simulationRoutes(ruleEvaluationService)
    }
}
