package studio.tearule.api.dto

import kotlinx.serialization.Serializable
import studio.tearule.domain.Severity

@Serializable
data class RuleResultDto(
    val ruleId: Long,
    val result: String,
    val severity: Severity,
    val message: String,
)

@Serializable
data class SimulationResponse(
    val teaLotId: Long,
    val shippable: Boolean,
    val results: List<RuleResultDto>,
)
