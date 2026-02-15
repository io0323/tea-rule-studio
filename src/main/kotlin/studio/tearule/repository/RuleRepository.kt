package studio.tearule.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.RuleResponse
import studio.tearule.db.tables.Rules
import studio.tearule.domain.Severity
import studio.tearule.rules.RuleDslParser

class RuleRepository {
    fun create(request: CreateRuleRequest): RuleResponse =
        transaction {
            // Validate DSL by parsing it
            RuleDslParser.parse(request.dsl)

            val id = Rules.insertAndGetId {
                it[name] = request.name
                it[dsl] = request.dsl
                it[severity] = request.severity.name
            }.value

            RuleResponse(
                id = id,
                name = request.name,
                dsl = request.dsl,
                severity = request.severity,
            )
        }

    fun findAll(): List<RuleResponse> =
        transaction {
            Rules.selectAll().map(::toRuleResponse)
        }

    fun findById(id: Long): RuleResponse? =
        transaction {
            Rules.select { Rules.id eq id }
                .limit(1)
                .firstOrNull()
                ?.let(::toRuleResponse)
        }

    fun deleteById(id: Long): Boolean =
        transaction {
            Rules.deleteWhere { Rules.id eq id } > 0
        }

    fun deleteByIds(ids: List<Long>): Int =
        transaction {
            Rules.deleteWhere { Rules.id inList ids }
        }

    private fun toRuleResponse(row: ResultRow): RuleResponse =
        RuleResponse(
            id = row[Rules.id].value,
            name = row[Rules.name],
            dsl = row[Rules.dsl],
            severity = Severity.valueOf(row[Rules.severity]),
        )
}
