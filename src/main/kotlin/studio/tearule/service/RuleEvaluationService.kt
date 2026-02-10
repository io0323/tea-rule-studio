package studio.tearule.service

import studio.tearule.api.dto.RuleResultDto
import studio.tearule.api.dto.SimulationResponse
import studio.tearule.domain.Severity
import studio.tearule.repository.RuleRepository
import studio.tearule.repository.TeaLotRepository
import studio.tearule.rules.RuleDslParser
import studio.tearule.rules.RuleEvaluation
import studio.tearule.rules.TeaLotSnapshot

class RuleEvaluationService(
    private val ruleRepository: RuleRepository,
    private val teaLotRepository: TeaLotRepository,
) {
    fun simulate(teaLotId: Long): SimulationResponse {
        val teaLot = teaLotRepository.findById(teaLotId)
            ?: error("tea lot not found")

        val snapshot = TeaLotSnapshot(
            moisture = teaLot.moisture,
            pesticideLevel = teaLot.pesticideLevel,
            aromaScore = teaLot.aromaScore,
        )

        val results = ruleRepository.findAll().map { rule ->
            val compiled = RuleDslParser.parse(rule.dsl)
            val evaluation = compiled.evaluate(snapshot)

            when (evaluation) {
                is RuleEvaluation.Pass ->
                    RuleResultDto(
                        ruleId = rule.id,
                        result = "PASS",
                        severity = rule.severity,
                        message = "${rule.name}: pass",
                    )

                is RuleEvaluation.Fail ->
                    RuleResultDto(
                        ruleId = rule.id,
                        result = "FAIL",
                        severity = rule.severity,
                        message = "${rule.name}: ${evaluation.message}",
                    )
            }
        }

        val hasBlockFail = results.any { it.result == "FAIL" && it.severity == Severity.BLOCK }

        return SimulationResponse(
            teaLotId = teaLotId,
            shippable = !hasBlockFail,
            results = results,
        )
    }
}
