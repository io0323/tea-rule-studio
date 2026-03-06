package studio.tearule.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.routes.simulationRoutes
import studio.tearule.db.tables.Rules
import studio.tearule.db.tables.TeaLots
import studio.tearule.repository.RuleRepository
import studio.tearule.repository.TeaLotRepository
import studio.tearule.service.RuleEvaluationService
import studio.tearule.api.dto.ApiResponse
import studio.tearule.api.dto.SimulationResponse
import studio.tearule.api.dto.BulkSimulationResponse
import studio.tearule.api.dto.BulkSimulationRequest
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.domain.Severity
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class SimulationRoutesIntegrationTest {

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Rules, TeaLots)
        }
    }

    @AfterEach
    fun teardown() {
        transaction {
            SchemaUtils.drop(Rules, TeaLots)
        }
    }

    @Test
    fun `POST simulate teaLotId returns simulation response`() = testApplication {
        val ruleRepository = RuleRepository()
        val teaLotRepository = TeaLotRepository()
        val ruleEvaluationService = RuleEvaluationService(ruleRepository, teaLotRepository)
        application {
            routing {
                simulationRoutes(ruleEvaluationService)
            }
        }
        // insert data
        ruleRepository.create(CreateRuleRequest("Rule1", "dsl", Severity.MEDIUM))
        val teaLotId = teaLotRepository.create(CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)).id

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

        val response = client.post("/simulate/${teaLotId}")
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<SimulationResponse>>()
        assertTrue(apiResponse.success)
        assertEquals(1, apiResponse.data?.results?.size)
    }

    @Test
    fun `POST simulate bulk returns bulk simulation response`() = testApplication {
        val ruleRepository = RuleRepository()
        val teaLotRepository = TeaLotRepository()
        val ruleEvaluationService = RuleEvaluationService(ruleRepository, teaLotRepository)
        application {
            routing {
                simulationRoutes(ruleEvaluationService)
            }
        }
        // insert data
        ruleRepository.create(CreateRuleRequest("Rule1", "dsl", Severity.MEDIUM))
        val teaLot = teaLotRepository.create(CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9))

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

        val response = client.post("/simulate") {
            contentType(ContentType.Application.Json)
            setBody(BulkSimulationRequest(listOf(teaLot.id)))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<BulkSimulationResponse>>()
        assertTrue(apiResponse.success)
        assertEquals(1, apiResponse.data?.results?.size)
    }

}
