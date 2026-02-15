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
            // Validate input data
            validateTeaLotRequest(request)

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

    private fun validateTeaLotRequest(request: CreateTeaLotRequest) {
        require(request.lotCode.isNotBlank()) { "lotCode must not be blank" }
        require(request.origin.isNotBlank()) { "origin must not be blank" }
        require(request.variety.isNotBlank()) { "variety must not be blank" }
        require(request.moisture in 0.0..100.0) { "moisture must be between 0.0 and 100.0" }
        require(request.pesticideLevel in 0.0..100.0) { "pesticideLevel must be between 0.0 and 100.0" }
        require(request.aromaScore in 1..10) { "aromaScore must be between 1 and 10" }
    }
