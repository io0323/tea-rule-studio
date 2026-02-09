package studio.tearule.seed

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import studio.tearule.db.tables.Rules
import studio.tearule.db.tables.TeaLots

object InitialData {
    fun seedIfEmpty() {
        transaction {
            val hasTeaLots = TeaLots.selectAll().limit(1).firstOrNull() != null
            val hasRules = Rules.selectAll().limit(1).firstOrNull() != null

            if (!hasTeaLots) {
                seedTeaLots()
            }
            if (!hasRules) {
                seedRules()
            }
        }
    }

    private fun seedTeaLots() {
        val rows = listOf(
            Triple("LOT-2026-001", "Shizuoka", "Yabukita") to TeaLotNumbers(8.8, 0.10, 78),
            Triple("LOT-2026-002", "Uji", "Samidori") to TeaLotNumbers(9.6, 0.08, 82),
            Triple("LOT-2026-003", "Kagoshima", "Yutakamidori") to TeaLotNumbers(10.2, 0.12, 74),
            Triple("LOT-2026-004", "Miyazaki", "Saemidori") to TeaLotNumbers(8.4, 0.18, 80),
            Triple("LOT-2026-005", "Shizuoka", "Okumidori") to TeaLotNumbers(9.1, 0.05, 69),
        )

        rows.forEach { (meta, nums) ->
            TeaLots.insert {
                it[lotCode] = meta.first
                it[origin] = meta.second
                it[variety] = meta.third
                it[moisture] = nums.moisture
                it[pesticideLevel] = nums.pesticideLevel
                it[aromaScore] = nums.aromaScore
            }
        }
    }

    private fun seedRules() {
        Rules.insert {
            it[name] = "Moisture Check"
            it[dsl] = "rule(\"Moisture Check\") { whenMoisture { it > 9.0 } then BLOCK }"
            it[severity] = "BLOCK"
        }
        Rules.insert {
            it[name] = "Pesticide Check"
            it[dsl] = "rule(\"Pesticide Check\") { whenPesticideLevel { it > 0.15 } then WARNING }"
            it[severity] = "WARNING"
        }
        Rules.insert {
            it[name] = "Aroma Check"
            it[dsl] = "rule(\"Aroma Check\") { whenAromaScore { it < 70 } then INFO }"
            it[severity] = "INFO"
        }
    }

    private data class TeaLotNumbers(
        val moisture: Double,
        val pesticideLevel: Double,
        val aromaScore: Int,
    )
}
