package studio.tearule.routes

import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.server.plugins.routing.Routing
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.routes.teaLotRoutes
import studio.tearule.db.tables.TeaLots
import studio.tearule.repository.TeaLotRepository
import kotlin.test.assertEquals

class TeaLotRoutesIntegrationTest {

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(TeaLots)
        }
    }

    @AfterEach
    fun teardown() {
        transaction {
            SchemaUtils.drop(TeaLots)
        }
    }

    @Test
    fun `GET tea-lots returns all tea lots`() = withTestApplication {
        val teaLotRepository = TeaLotRepository()
        routing {
            teaLotRoutes(teaLotRepository)
        }

        // insert data
        teaLotRepository.create(CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9))

        // test
        handleRequest(HttpMethod.Get, "/tea-lots").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val responseBody = response.content
            assert(responseBody?.contains("LOT001") == true)
        }
    }

    @Test
    fun `POST tea-lots creates new tea lot`() = withTestApplication {
        val teaLotRepository = TeaLotRepository()
        routing {
            teaLotRoutes(teaLotRepository)
        }

        // test
        handleRequest(HttpMethod.Post, "/tea-lots") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"lotCode":"LOT002","origin":"Japan","variety":"Black","moisture":10.0,"pesticideLevel":1.5,"aromaScore":8}""")
        }.apply {
            assertEquals(HttpStatusCode.Created, response.status())
            val responseBody = response.content
            assert(responseBody?.contains("LOT002") == true)
        }
    }

    @Test
    fun `GET tea-lots id returns specific tea lot`() = withTestApplication {
        val teaLotRepository = TeaLotRepository()
        routing {
            teaLotRoutes(teaLotRepository)
        }

        // insert data
        val created = teaLotRepository.create(CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9))

        // test
        handleRequest(HttpMethod.Get, "/tea-lots/${created.id}").apply {
            assertEquals(HttpStatusCode.OK, response.status())
            val responseBody = response.content
            assert(responseBody?.contains("LOT001") == true)
        }
    }

}
