package studio.tearule.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.ImportRulesRequest
import studio.tearule.api.dto.ImportRulesResponse
import studio.tearule.repository.RuleRepository
import studio.tearule.api.validation.ValidationUtils

fun Route.ruleRoutes(ruleRepository: RuleRepository) {
    route("/rules") {
        get {
            val rules = ruleRepository.findAll()
            call.respond(rules)
        }

        post {
            val request = call.receive<CreateRuleRequest>()
            val validationResult = ValidationUtils.validateCreateRuleRequest(request)
            if (validationResult is ValidationUtils.ValidationResult.Invalid) {
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

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

            val deleted = ruleRepository.deleteById(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "rule not found"))
            }
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
        if (validationResult is ValidationUtils.ValidationResult.Invalid) {
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
