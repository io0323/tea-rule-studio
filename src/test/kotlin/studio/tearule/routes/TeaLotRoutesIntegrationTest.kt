package studio.tearule.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.*
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
import studio.tearule.api.dto.ApiResponse
import studio.tearule.api.dto.TeaLotResponse
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `GET tea-lots returns all tea lots`() = testApplication {
        val teaLotRepository = TeaLotRepository()
        application {
            routing {
                teaLotRoutes(teaLotRepository)
            }
        }
        // insert data
        teaLotRepository.create(CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9))

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

        val response = client.get("/tea-lots")
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<List<TeaLotResponse>>>()
        assertTrue(apiResponse.success)
        assertEquals(1, apiResponse.data?.size)
        assertEquals("LOT001", apiResponse.data?.first()?.lotCode)
    }

    @Test
    fun `POST tea-lots creates new tea lot`() = testApplication {
        val teaLotRepository = TeaLotRepository()
        application {
            routing {
                teaLotRoutes(teaLotRepository)
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

        val response = client.post("/tea-lots") {
            contentType(ContentType.Application.Json)
            setBody(CreateTeaLotRequest("LOT002", "Japan", "Black", 10.0, 1.5, 8))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val apiResponse = response.body<ApiResponse<TeaLotResponse>>()
        assertTrue(apiResponse.success)
        assertEquals("LOT002", apiResponse.data?.lotCode)
    }

    @Test
    fun `GET tea-lots id returns specific tea lot`() = testApplication {
        val teaLotRepository = TeaLotRepository()
        application {
            routing {
                teaLotRoutes(teaLotRepository)
            }
        }
        // insert data
        val created = teaLotRepository.create(CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9))

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

        val response = client.get("/tea-lots/${created.id}")
        assertEquals(HttpStatusCode.OK, response.status)
        val apiResponse = response.body<ApiResponse<TeaLotResponse>>()
        assertTrue(apiResponse.success)
        assertEquals("LOT001", apiResponse.data?.lotCode)
    }

}
