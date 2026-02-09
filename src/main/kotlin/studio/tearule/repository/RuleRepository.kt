package studio.tearule.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.RuleResponse
import studio.tearule.db.tables.Rules
import studio.tearule.domain.Severity

class RuleRepository {
    fun create(request: CreateRuleRequest): RuleResponse =
        transaction {
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

    private fun toRuleResponse(row: ResultRow): RuleResponse =
        RuleResponse(
            id = row[Rules.id].value,
            name = row[Rules.name],
            dsl = row[Rules.dsl],
            severity = Severity.valueOf(row[Rules.severity]),
        )
}
