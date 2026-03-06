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
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.routes.ruleRoutes
import studio.tearule.db.tables.Rules
import studio.tearule.domain.Severity
import studio.tearule.repository.RuleRepository
import studio.tearule.api.dto.ApiResponse
import studio.tearule.api.dto.RuleResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class RuleRoutesIntegrationTest {

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Rules)
        }
    }

    @AfterEach
    fun teardown() {
        transaction {
            SchemaUtils.drop(Rules)
        }
    }

    @Test
    fun `GET rules returns all rules`() = testApplication {
        val ruleRepository = RuleRepository()
        application {
            routing {
                ruleRoutes(ruleRepository)
            }
        }
        // insert data
        ruleRepository.create(CreateRuleRequest("Rule1", "dsl", Severity.MEDIUM))

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

        val response = client.get("/rules")
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<List<RuleResponse>>>()
        assertTrue(apiResponse.success)
        assertEquals(1, apiResponse.data?.size)
        assertEquals("Rule1", apiResponse.data?.first()?.name)
    }

    @Test
    fun `POST rules creates new rule`() = testApplication {
        val ruleRepository = RuleRepository()
        application {
            routing {
                ruleRoutes(ruleRepository)
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

        val response = client.post("/rules") {
            contentType(ContentType.Application.Json)
            setBody(CreateRuleRequest("Rule2", "some dsl", Severity.LOW))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val apiResponse = response.body<ApiResponse<RuleResponse>>()
        assertTrue(apiResponse.success)
        assertEquals("Rule2", apiResponse.data?.name)
    }

    @Test
    fun `GET rules id returns specific rule`() = testApplication {
        val ruleRepository = RuleRepository()
        application {
            routing {
                ruleRoutes(ruleRepository)
            }
        }
        // insert data
        val created = ruleRepository.create(CreateRuleRequest("Rule1", "dsl", Severity.MEDIUM))

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

        val response = client.get("/rules/${created.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<RuleResponse>>()
        assertTrue(apiResponse.success)
        assertEquals("Rule1", apiResponse.data?.name)
    }

}
