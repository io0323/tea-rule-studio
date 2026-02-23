package studio.tearule.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.dto.UpdateTeaLotRequest
import studio.tearule.db.tables.TeaLots
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TeaLotRepositoryTest {

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
    fun `create should insert and return TeaLotResponse`() {
        val repo = TeaLotRepository()
        val request = CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)
        val result = repo.create(request)
        assertNotNull(result)
        assertEquals("LOT001", result.lotCode)
        assertEquals("China", result.origin)
        assertEquals("Green", result.variety)
        assertEquals(12.5, result.moisture)
        assertEquals(2.0, result.pesticideLevel)
        assertEquals(9, result.aromaScore)
        assertNotNull(result.id)
    }

    @Test
    fun `findAll should return all tea lots`() {
        val repo = TeaLotRepository()
        val request1 = CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)
        val request2 = CreateTeaLotRequest("LOT002", "Japan", "Black", 10.0, 1.5, 8)
        repo.create(request1)
        repo.create(request2)
        val results = repo.findAll()
        assertEquals(2, results.size)
        assertEquals("LOT001", results[0].lotCode)
        assertEquals("LOT002", results[1].lotCode)
    }

    @Test
    fun `findById should return tea lot if exists`() {
        val repo = TeaLotRepository()
        val request = CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)
        val created = repo.create(request)
        val found = repo.findById(created.id)
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("LOT001", found.lotCode)
    }

    @Test
    fun `findById should return null if not exists`() {
        val repo = TeaLotRepository()
        val found = repo.findById(999L)
        assertNull(found)
    }

    @Test
    fun `update should modify existing tea lot`() {
        val repo = TeaLotRepository()
        val request = CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)
        val created = repo.create(request)
        val updateRequest = UpdateTeaLotRequest(lotCode = "LOT001_UPDATED", moisture = 15.0)
        val updated = repo.update(created.id, updateRequest)
        assertNotNull(updated)
        assertEquals(created.id, updated.id)
        assertEquals("LOT001_UPDATED", updated.lotCode)
        assertEquals("China", updated.origin) // unchanged
        assertEquals(15.0, updated.moisture)
        assertEquals(2.0, updated.pesticideLevel) // unchanged
    }

    @Test
    fun `update should return null if not exists`() {
        val repo = TeaLotRepository()
        val updateRequest = UpdateTeaLotRequest(lotCode = "LOT001")
        val updated = repo.update(999L, updateRequest)
        assertNull(updated)
    }

    @Test
    fun `deleteById should return true if deleted`() {
        val repo = TeaLotRepository()
        val request = CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)
        val created = repo.create(request)
        val deleted = repo.deleteById(created.id)
        assertEquals(true, deleted)
        val found = repo.findById(created.id)
        assertNull(found)
    }

    @Test
    fun `deleteById should return false if not exists`() {
        val repo = TeaLotRepository()
        val deleted = repo.deleteById(999L)
        assertEquals(false, deleted)
    }

    @Test
    fun `deleteByIds should delete multiple tea lots`() {
        val repo = TeaLotRepository()
        val request1 = CreateTeaLotRequest("LOT001", "China", "Green", 12.5, 2.0, 9)
        val request2 = CreateTeaLotRequest("LOT002", "Japan", "Black", 10.0, 1.5, 8)
        val created1 = repo.create(request1)
        val created2 = repo.create(request2)
        val count = repo.deleteByIds(listOf(created1.id, created2.id))
        assertEquals(2, count)
        val found1 = repo.findById(created1.id)
        val found2 = repo.findById(created2.id)
        assertNull(found1)
        assertNull(found2)
    }
}
