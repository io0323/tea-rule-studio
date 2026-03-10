package studio.tearule

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.util.AttributeKey
import studio.tearule.db.DatabaseFactory
import studio.tearule.repository.RuleRepository
import studio.tearule.repository.TeaLotRepository
import studio.tearule.seed.InitialData
import studio.tearule.service.RuleEvaluationService
import studio.tearule.api.dto.ApiResponse
import io.ktor.server.plugins.statuspages.exception
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.netty.EngineMain
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
import kotlinx.serialization.ExperimentalSerializationApi

fun main(args: Array<String>) {
    // Set port from environment variable if available
    System.getenv("PORT")?.toIntOrNull()?.let { port ->
        System.setProperty("ktor.deployment.port", port.toString())
    }

    EngineMain.main(args)
}

@OptIn(ExperimentalSerializationApi::class)
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

        // Performance monitoring: log execution time for all requests
        intercept(ApplicationCallPipeline.Monitoring) {
            val startTime = System.currentTimeMillis()
            call.attributes.put(AttributeKey("startTime"), startTime)
        }

        staticResources("/static", "static")
        get("/") {
            call.respondRedirect("/static/index.html")
        }

        get("/health") {
            val dbConnected = DatabaseFactory.checkConnection()
            val rulesCount = if (dbConnected) ruleRepository.findAll().size else 0
            val teaLotsCount = if (dbConnected) teaLotRepository.findAll().size else 0
            if (dbConnected) {
                log.info("Health check: Database connected, rules: $rulesCount, tea lots: $teaLotsCount")
                call.respond(ApiResponse<Map<String, Any>>(success = dbConnected, data = mapOf(
                    "status" to if (dbConnected) "ok" else "error",
                    "database" to if (dbConnected) "connected" else "disconnected",
                    "rules_count" to rulesCount,
                    "tea_lots_count" to teaLotsCount
                ), error = if (!dbConnected) "Database disconnected" else null))
            } else {
                call.respond(ApiResponse<Map<String, Any>>(success = dbConnected, data = mapOf(
                    "status" to if (dbConnected) "ok" else "error",
                    "database" to if (dbConnected) "connected" else "disconnected",
                    "rules_count" to rulesCount,
                    "tea_lots_count" to teaLotsCount
                ), error = if (!dbConnected) "Database disconnected" else null))
            }
        }

        ruleRoutes(ruleRepository)
        teaLotRoutes(teaLotRepository)
        simulationRoutes(ruleEvaluationService)
    }
}
