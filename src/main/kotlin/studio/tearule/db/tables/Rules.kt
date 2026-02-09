package studio.tearule.db.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object Rules : LongIdTable("rules") {
    val name = varchar("name", 200)
    val dsl = text("dsl")
    val severity = varchar("severity", 16)
}
