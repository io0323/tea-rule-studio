package studio.tearule.api.validation

import studio.tearule.api.dto.*

/**
 * Validation utility functions for API requests
 */
object ValidationUtils {

    fun validateCreateTeaLotRequest(request: CreateTeaLotRequest): ValidationResult {
        val errors = mutableListOf<String>()

        if (request.lotCode.isBlank()) {
            errors.add("lotCode cannot be blank")
        }

        if (request.origin.isBlank()) {
            errors.add("origin cannot be blank")
        }

        if (request.variety.isBlank()) {
            errors.add("variety cannot be blank")
        }

        if (request.moisture < 0.0 || request.moisture > 100.0) {
            errors.add("moisture must be between 0.0 and 100.0")
        }

        if (request.pesticideLevel < 0.0) {
            errors.add("pesticideLevel cannot be negative")
        }

        if (request.aromaScore < 0 || request.aromaScore > 100) {
            errors.add("aromaScore must be between 0 and 100")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun validateCreateRuleRequest(request: CreateRuleRequest): ValidationResult {
        val errors = mutableListOf<String>()

        if (request.name.isBlank()) {
            errors.add("name cannot be blank")
        }

        if (request.dsl.isBlank()) {
            errors.add("dsl cannot be blank")
        }

        // Note: severity is an enum, so it's automatically validated by serialization

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun validateBulkSimulationRequest(request: BulkSimulationRequest): ValidationResult {
        val errors = mutableListOf<String>()

        if (request.teaLotIds.isEmpty()) {
            errors.add("teaLotIds cannot be empty")
        }

        if (request.teaLotIds.any { it <= 0 }) {
            errors.add("all teaLotIds must be positive numbers")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun validateImportTeaLotsRequest(request: ImportTeaLotsRequest): ValidationResult {
        val errors = mutableListOf<String>()

        if (request.teaLots.isEmpty()) {
            errors.add("teaLots cannot be empty")
        }

        request.teaLots.forEachIndexed { index, teaLot ->
            val teaLotErrors = validateCreateTeaLotRequest(teaLot)
            if (teaLotErrors is ValidationResult.Invalid) {
                errors.addAll(teaLotErrors.errors.map { "teaLots[$index]: $it" })
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun validateImportRulesRequest(request: ImportRulesRequest): ValidationResult {
        val errors = mutableListOf<String>()

        if (request.rules.isEmpty()) {
            errors.add("rules cannot be empty")
        }

        request.rules.forEachIndexed { index, rule ->
            val ruleErrors = validateCreateRuleRequest(rule)
            if (ruleErrors is ValidationResult.Invalid) {
                errors.addAll(ruleErrors.errors.map { "rules[$index]: $it" })
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}
