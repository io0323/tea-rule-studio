package studio.tearule.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class BulkSimulationRequest(
    val teaLotIds: List<Long>
)

@Serializable
data class BulkSimulationResponse(
    val results: List<SimulationResponse>
)

@Serializable
data class ImportRulesRequest(
    val rules: List<CreateRuleRequest>
)

@Serializable
data class ImportTeaLotsRequest(
    val teaLots: List<CreateTeaLotRequest>
)

@Serializable
data class ImportRulesResponse(
    val imported: Int,
    val rules: List<RuleResponse>
)

@Serializable
data class ImportTeaLotsResponse(
    val imported: Int,
    val teaLots: List<TeaLotResponse>
)
