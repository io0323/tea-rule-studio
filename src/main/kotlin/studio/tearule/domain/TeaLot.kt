package studio.tearule.domain

import kotlinx.serialization.Serializable

@Serializable
data class TeaLot(
    val id: Long,
    val lotCode: String,
    val origin: String,
    val variety: String,
    val moisture: Double,
    val pesticideLevel: Double,
    val aromaScore: Int,
)
