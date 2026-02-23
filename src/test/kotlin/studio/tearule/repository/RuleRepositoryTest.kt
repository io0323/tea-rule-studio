package studio.tearule.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.UpdateRuleRequest
import studio.tearule.db.tables.Rules
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RuleRepositoryTest {

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
    fun `create should insert and return RuleResponse`() {
        val repo = RuleRepository()
        val request = CreateRuleRequest("Rule1", "some dsl", 5)
        val result = repo.create(request)
        assertNotNull(result)
        assertEquals("Rule1", result.name)
        assertEquals("some dsl", result.dsl)
        assertEquals(5, result.severity)
        assertNotNull(result.id)
    }

    @Test
    fun `findAll should return all rules`() {
        val repo = RuleRepository()
        val request1 = CreateRuleRequest("Rule1", "dsl1", 5)
        val request2 = CreateRuleRequest("Rule2", "dsl2", 3)
        repo.create(request1)
        repo.create(request2)
        val results = repo.findAll()
        assertEquals(2, results.size)
        assertEquals("Rule1", results[0].name)
        assertEquals("Rule2", results[1].name)
    }

    @Test
    fun `findById should return rule if exists`() {
        val repo = RuleRepository()
        val request = CreateRuleRequest("Rule1", "dsl", 5)
        val created = repo.create(request)
        val found = repo.findById(created.id)
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Rule1", found.name)
    }

    @Test
    fun `findById should return null if not exists`() {
        val repo = RuleRepository()
        val found = repo.findById(999L)
        assertNull(found)
    }

    @Test
    fun `update should modify existing rule`() {
        val repo = RuleRepository()
        val request = CreateRuleRequest("Rule1", "dsl", 5)
        val created = repo.create(request)
        val updateRequest = UpdateRuleRequest(name = "Rule1_updated", severity = 10)
        val updated = repo.update(created.id, updateRequest)
        assertNotNull(updated)
        assertEquals(created.id, updated.id)
        assertEquals("Rule1_updated", updated.name)
        assertEquals("dsl", updated.dsl) // unchanged
        assertEquals(10, updated.severity)
    }

    @Test
    fun `update should return null if not exists`() {
        val repo = RuleRepository()
        val updateRequest = UpdateRuleRequest(name = "Rule1")
        val updated = repo.update(999L, updateRequest)
        assertNull(updated)
    }

    @Test
    fun `deleteById should return true if deleted`() {
        val repo = RuleRepository()
        val request = CreateRuleRequest("Rule1", "dsl", 5)
        val created = repo.create(request)
        val deleted = repo.deleteById(created.id)
        assertEquals(true, deleted)
        val found = repo.findById(created.id)
        assertNull(found)
    }

    @Test
    fun `deleteById should return false if not exists`() {
        val repo = RuleRepository()
        val deleted = repo.deleteById(999L)
        assertEquals(false, deleted)
    }

    @Test
    fun `deleteByIds should delete multiple rules`() {
        val repo = RuleRepository()
        val request1 = CreateRuleRequest("Rule1", "dsl1", 5)
        val request2 = CreateRuleRequest("Rule2", "dsl2", 3)
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
