package studio.tearule.domain

import kotlinx.serialization.Serializable

@Serializable
data class Rule(
    val id: Long,
    val name: String,
    val dsl: String,
    val severity: Severity,
)

enum class Severity {
    INFO,
    WARNING,
    BLOCK,
}
