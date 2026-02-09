package studio.tearule.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object TeaLots : LongIdTable("tea_lots") {
    val lotCode = varchar("lot_code", 64).uniqueIndex()
    val origin = varchar("origin", 128)
    val variety = varchar("variety", 128)
    val moisture = double("moisture")
    val pesticideLevel = double("pesticide_level")
    val aromaScore = integer("aroma_score")
}
