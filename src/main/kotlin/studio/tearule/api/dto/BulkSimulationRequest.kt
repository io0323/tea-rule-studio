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
