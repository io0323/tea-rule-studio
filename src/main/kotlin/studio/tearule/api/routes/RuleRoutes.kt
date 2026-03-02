package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.http.HttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.method
import studio.tearule.api.dto.UpdateRuleRequest
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.ImportRulesRequest
import studio.tearule.api.dto.ImportRulesResponse
import studio.tearule.api.validation.ValidationUtils
import studio.tearule.api.validation.ValidationResult
import studio.tearule.repository.RuleRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.ruleRoutes(ruleRepository: RuleRepository) {
    val logger = LoggerFactory.getLogger("RuleRoutes")
    route("/rules") {
        get {
            val rules = ruleRepository.findAll()
            call.respond(rules)
        }

        post {
            val request = call.receive<CreateRuleRequest>()
            val validationResult = ValidationUtils.validateCreateRuleRequest(request)
            if (validationResult is ValidationResult.Invalid) {
                logger.warn("Create rule validation failed: {}", validationResult.errors.joinToString(", "))
                return@post call.respond(HttpStatusCode.BadRequest, mapOf(
                    "error" to "Validation failed",
                    "details" to validationResult.errors
                ))
            }
            val rule = ruleRepository.create(request)
            call.respond(HttpStatusCode.Created, rule)
        }

        delete {
            val ids = call.receive<List<Long>>()
            val count = ruleRepository.deleteByIds(ids)
            call.respond(mapOf("deleted" to count))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

            val rule = ruleRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "rule not found"))

            call.respond(rule)
        }
    }

    put("/rules/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

        val request = call.receive<UpdateRuleRequest>()
        ValidationUtils.validateUpdateRuleRequest(request).let { result ->
            when (result) {
                is ValidationResult.Invalid -> throw IllegalArgumentException("Validation failed: ${result.errors.joinToString()}")
                is ValidationResult.Valid -> {}
            }
        }

        val updatedRule = ruleRepository.update(id, request)
            ?: return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "rule not found"))

        call.respond(updatedRule)
    }

    delete("/rules/{id}") {
        val id = call.parameters["id"]?.toLongOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

        val deleted = ruleRepository.deleteById(id)
        if (deleted) {
            call.respond(HttpStatusCode.NoContent)
        } else {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "rule not found"))
        }
    }

    get("/export/rules") {
        val rules = ruleRepository.findAll()
        call.response.headers.append("Content-Disposition", "attachment; filename=\"rules.json\"")
        call.response.headers.append("Content-Type", "application/json")
        call.respond(rules)
    }

    post("/import/rules") {
        val request = call.receive<ImportRulesRequest>()
        val validationResult = ValidationUtils.validateImportRulesRequest(request)
        if (validationResult is ValidationResult.Invalid) {
            return@post call.respond(HttpStatusCode.BadRequest, mapOf(
                "error" to "Validation failed",
                "details" to validationResult.errors
            ))
        }
        val importedRules = request.rules.map { ruleRepository.create(it) }
        val response = ImportRulesResponse(importedRules.size, importedRules)
        call.respond(HttpStatusCode.Created, response)
    }
}
