package studio.tearule

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.plugins.cors.routing.CORS
import org.jetbrains.exposed.exceptions.ExposedSQLException
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.*
import io.ktor.http.ContentType
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

    install(CORS) {
        allowHost("localhost:8080")
        allowHost("127.0.0.1:8080")
        allowHost("localhost:3000") // For development frontend
        allowHost("127.0.0.1:3000") // For development frontend
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowCredentials = true
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respondText("{\"message\": \"Invalid request\", \"status\": 400}", ContentType.Application.Json, HttpStatusCode.BadRequest)
        }
        exception<ExposedSQLException> { call, cause ->
            call.respondText("{\"message\": \"Database error\", \"status\": 500}", ContentType.Application.Json, HttpStatusCode.InternalServerError)
        }
        exception<Exception> { call, cause ->
            call.respondText("{\"message\": \"Internal server error\", \"status\": 500}", ContentType.Application.Json, HttpStatusCode.InternalServerError)
        }
        exception<Throwable> { call, cause ->
            call.respondText("{\"message\": \"Unexpected error\", \"status\": 500}", ContentType.Application.Json, HttpStatusCode.InternalServerError)
        }
    }

    routing {
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
            call.respond(mapOf("status" to "ok"))
        }

        ruleRoutes(ruleRepository)
        teaLotRoutes(teaLotRepository)
        simulationRoutes(ruleEvaluationService)
    }
}
