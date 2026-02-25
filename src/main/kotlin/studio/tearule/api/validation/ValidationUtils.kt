package studio.tearule.api.validation

import studio.tearule.api.dto.CreateTeaLotRequest
import studio.tearule.api.dto.UpdateTeaLotRequest
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.UpdateRuleRequest

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}

object ValidationUtils {
    
    fun validateCreateTeaLotRequest(request: CreateTeaLotRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (request.lotCode.isBlank()) {
            errors.add("lotCode must not be blank")
        } else if (request.lotCode.length > 50) {
            errors.add("lotCode must not exceed 50 characters")
        }
        
        if (request.origin.isBlank()) {
            errors.add("origin must not be blank")
        } else if (request.origin.length > 100) {
            errors.add("origin must not exceed 100 characters")
        }
        
        if (request.variety.isBlank()) {
            errors.add("variety must not be blank")
        } else if (request.variety.length > 50) {
            errors.add("variety must not exceed 50 characters")
        }
        
        if (request.moisture < 0.0 || request.moisture > 100.0) {
            errors.add("moisture must be between 0.0 and 100.0")
        }
        
        if (request.pesticideLevel < 0.0 || request.pesticideLevel > 100.0) {
            errors.add("pesticideLevel must be between 0.0 and 100.0")
        }
        
        if (request.aromaScore < 1 || request.aromaScore > 10) {
            errors.add("aromaScore must be between 1 and 10")
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    fun validateUpdateTeaLotRequest(request: UpdateTeaLotRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        request.lotCode?.let { lotCode ->
            if (lotCode.isBlank()) {
                errors.add("lotCode must not be blank")
            } else if (lotCode.length > 50) {
                errors.add("lotCode must not exceed 50 characters")
            }
        }
        
        request.origin?.let { origin ->
            if (origin.isBlank()) {
                errors.add("origin must not be blank")
            } else if (origin.length > 100) {
                errors.add("origin must not exceed 100 characters")
            }
        }
        
        request.variety?.let { variety ->
            if (variety.isBlank()) {
                errors.add("variety must not be blank")
            } else if (variety.length > 50) {
                errors.add("variety must not exceed 50 characters")
            }
        }
        
        request.moisture?.let { moisture ->
            if (moisture < 0.0 || moisture > 100.0) {
                errors.add("moisture must be between 0.0 and 100.0")
            }
        }
        
        request.pesticideLevel?.let { pesticideLevel ->
            if (pesticideLevel < 0.0 || pesticideLevel > 100.0) {
                errors.add("pesticideLevel must be between 0.0 and 100.0")
            }
        }
        
        request.aromaScore?.let { aromaScore ->
            if (aromaScore < 1 || aromaScore > 10) {
                errors.add("aromaScore must be between 1 and 10")
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    fun validateCreateRuleRequest(request: CreateRuleRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (request.name.isBlank()) {
            errors.add("name must not be blank")
        } else if (request.name.length > 100) {
            errors.add("name must not exceed 100 characters")
        }
        
        if (request.dsl.isBlank()) {
            errors.add("dsl must not be blank")
        } else if (request.dsl.length > 1000) {
            errors.add("dsl must not exceed 1000 characters")
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
    
    fun validateUpdateRuleRequest(request: UpdateRuleRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        request.name?.let { name ->
            if (name.isBlank()) {
                errors.add("name must not be blank")
            } else if (name.length > 100) {
                errors.add("name must not exceed 100 characters")
            }
        }
        
        request.dsl?.let { dsl ->
            if (dsl.isBlank()) {
                errors.add("dsl must not be blank")
            } else if (dsl.length > 1000) {
                errors.add("dsl must not exceed 1000 characters")
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
