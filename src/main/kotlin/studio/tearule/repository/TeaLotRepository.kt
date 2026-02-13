package studio.tearule.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.dto.TeaLotResponse
import studio.tearule.db.tables.TeaLots

class TeaLotRepository {
    fun create(request: CreateTeaLotRequest): TeaLotResponse =
        transaction {
            val id = TeaLots.insertAndGetId {
                it[lotCode] = request.lotCode
                it[origin] = request.origin
                it[variety] = request.variety
                it[moisture] = request.moisture
                it[pesticideLevel] = request.pesticideLevel
                it[aromaScore] = request.aromaScore
            }.value

            TeaLotResponse(
                id = id,
                lotCode = request.lotCode,
                origin = request.origin,
                variety = request.variety,
                moisture = request.moisture,
                pesticideLevel = request.pesticideLevel,
                aromaScore = request.aromaScore,
            )
        }

    fun findAll(): List<TeaLotResponse> =
        transaction {
            TeaLots.selectAll().map(::toTeaLotResponse)
        }

    fun findById(id: Long): TeaLotResponse? =
        transaction {
            TeaLots.select { TeaLots.id eq id }
                .limit(1)
                .firstOrNull()
                ?.let(::toTeaLotResponse)
        }

    fun deleteById(id: Long): Boolean =
        transaction {
            TeaLots.deleteWhere { TeaLots.id eq id } > 0
        }

    fun deleteByIds(ids: List<Long>): Int =
        transaction {
            TeaLots.deleteWhere { TeaLots.id inList ids }
        }

    private fun toTeaLotResponse(row: ResultRow): TeaLotResponse =
        TeaLotResponse(
            id = row[TeaLots.id].value,
            lotCode = row[TeaLots.lotCode],
            origin = row[TeaLots.origin],
            variety = row[TeaLots.variety],
            moisture = row[TeaLots.moisture],
            pesticideLevel = row[TeaLots.pesticideLevel],
            aromaScore = row[TeaLots.aromaScore],
        )
}
