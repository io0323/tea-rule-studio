package studio.tearule.api.dto

import kotlinx.serialization.Serializable
import studio.tearule.domain.Severity

@Serializable
data class CreateRuleRequest(
    val name: String,
    val dsl: String,
    val severity: Severity,
)

@Serializable
data class UpdateRuleRequest(
    val name: String? = null,
    val dsl: String? = null,
    val severity: Severity? = null,
)

@Serializable
data class RuleResponse(
    val id: Long,
    val name: String,
    val dsl: String,
    val severity: Severity,
)

@Serializable
data class UpdateRuleRequest(
    val name: String? = null,
    val dsl: String? = null,
    val severity: Severity? = null,
)
