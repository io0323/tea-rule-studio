package studio.tearule.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTeaLotRequest(
    val lotCode: String,
    val origin: String,
    val variety: String,
    val moisture: Double,
    val pesticideLevel: Double,
    val aromaScore: Int,
)

@Serializable
data class TeaLotResponse(
    val id: Long,
    val lotCode: String,
    val origin: String,
    val variety: String,
    val moisture: Double,
    val pesticideLevel: Double,
    val aromaScore: Int,
)
