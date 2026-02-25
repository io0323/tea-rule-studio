package studio.tearule.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
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
import kotlin.test.assertEquals

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
    fun `GET rules returns all rules`() = withTestApplication {
        val ruleRepository = RuleRepository()
        
        // insert data
        ruleRepository.create(CreateRuleRequest("Rule1", "dsl", Severity.MEDIUM))

        // test
        handleRequest(HttpMethod.Get, "/rules").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val responseBody = response.content
            assert(responseBody?.contains("Rule1") == true)
        }
    }

    @Test
    fun `POST rules creates new rule`() = withTestApplication {
        val ruleRepository = RuleRepository()

        // test
        handleRequest(HttpMethod.Post, "/rules") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"name":"Rule2","dsl":"some dsl","severity":"LOW"}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, response.status())
            val responseBody = response.content
            assert(responseBody?.contains("Rule2") == true)
        }
    }

    @Test
    fun `GET rules id returns specific rule`() = withTestApplication {
        val ruleRepository = RuleRepository()
        
        // insert data
        val created = ruleRepository.create(CreateRuleRequest("Rule1", "dsl", Severity.MEDIUM))

        // test
        handleRequest(HttpMethod.Get, "/rules/${created.id}").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val responseBody = response.content
            assert(responseBody?.contains("Rule1") == true)
        }
    }

}
