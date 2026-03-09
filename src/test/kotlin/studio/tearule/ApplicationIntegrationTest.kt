package studio.tearule

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.junit.jupiter.api.Test
import studio.tearule.api.dto.ApiResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import studio.tearule.db.DatabaseFactory
import studio.tearule.api.routes.ruleRoutes
import studio.tearule.api.routes.teaLotRoutes
import studio.tearule.api.routes.simulationRoutes
import studio.tearule.repository.RuleRepository
import studio.tearule.repository.TeaLotRepository
import studio.tearule.service.RuleEvaluationService
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.client.request.get

class ApplicationIntegrationTest {

    @Test
    fun `GET health returns health status with counts`() = testApplication {
        application {
            this.routing {
                val ruleRepository = RuleRepository()
                val teaLotRepository = TeaLotRepository()
                val ruleEvaluationService = RuleEvaluationService(ruleRepository, teaLotRepository)

                get("/health") {
                    val dbConnected = DatabaseFactory.checkConnection()
                    val rulesCount = if (dbConnected) ruleRepository.findAll().size else 0
                    val teaLotsCount = if (dbConnected) teaLotRepository.findAll().size else 0
                    call.respond(ApiResponse<Map<String, Any>>(success = dbConnected, data = mapOf(
                        "status" to if (dbConnected) "ok" else "error",
                        "database" to if (dbConnected) "connected" else "disconnected",
                        "rules_count" to rulesCount,
                        "tea_lots_count" to teaLotsCount
                    ), error = if (!dbConnected) "Database disconnected" else null))
                }

                ruleRoutes(ruleRepository)
                teaLotRoutes(teaLotRepository)
                simulationRoutes(ruleEvaluationService)
            }
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    isLenient = false
                    ignoreUnknownKeys = false
                    explicitNulls = false
                })
            }
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<Map<String, Any>>>()
        assertTrue(apiResponse.success)
        assertEquals("ok", apiResponse.data?.get("status"))
        assertEquals("connected", apiResponse.data?.get("database"))
        assertTrue(apiResponse.data?.containsKey("rules_count") == true)
        assertTrue(apiResponse.data?.containsKey("tea_lots_count") == true)
    }

}
