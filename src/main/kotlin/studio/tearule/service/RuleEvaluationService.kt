package studio.tearule.service

import studio.tearule.api.dto.RuleResultDto
import studio.tearule.api.dto.RuleResponse
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

        val results: List<RuleResultDto> = (ruleRepository.findAll() as List<RuleResponse>).map { ruleResponse: RuleResponse ->
            val compiled = RuleDslParser.parse(ruleResponse.dsl)
            val evaluation = compiled.evaluate(snapshot)

            when (evaluation) {
                is RuleEvaluation.Pass ->
                    RuleResultDto(
                        ruleId = ruleResponse.id,
                        result = "PASS",
                        severity = ruleResponse.severity,
                        message = "${ruleResponse.name}: pass",
                    )

                is RuleEvaluation.Fail ->
                    RuleResultDto(
                        ruleId = ruleResponse.id,
                        result = "FAIL",
                        severity = ruleResponse.severity,
                        message = "${ruleResponse.name}: ${evaluation.message}",
                    )
            }
        }

        val hasBlockFail = results.any { it.result == "FAIL" && it.severity == Severity.HIGH }

        return SimulationResponse(
            teaLotId = teaLotId,
            shippable = !hasBlockFail,
            results = results,
        )
    }
}
