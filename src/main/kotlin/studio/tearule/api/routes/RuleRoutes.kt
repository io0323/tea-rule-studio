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
import io.ktor.server.plugins.openapi.*
import io.swagger.v3.oas.models.info.Info
import studio.tearule.api.dto.CreateRuleRequest
import studio.tearule.api.dto.ImportRulesRequest
import studio.tearule.api.dto.ImportRulesResponse
import studio.tearule.repository.RuleRepository

fun Route.ruleRoutes(ruleRepository: RuleRepository) {
    route("/rules") {
        get {
            val rules = ruleRepository.findAll()
            call.respond(rules)
        } openapi {
            summary = "Get all rules"
            description = "Retrieve a list of all tea rules"
            operationId = "getAllRules"
            response {
                HttpStatusCode.OK to {
                    description = "List of rules"
                    body<List<studio.tearule.api.dto.RuleResponse>>()
                }
            }
        }

        post {
            val request = call.receive<CreateRuleRequest>()
            val rule = ruleRepository.create(request)
            call.respond(HttpStatusCode.Created, rule)
        } openapi {
            summary = "Create a new rule"
            description = "Create a new tea rule with DSL validation"
            operationId = "createRule"
            requestBody {
                description = "Rule creation request"
                body<CreateRuleRequest>()
            }
            response {
                HttpStatusCode.Created to {
                    description = "Created rule"
                    body<studio.tearule.api.dto.RuleResponse>()
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid request or DSL syntax error"
                }
            }
        }

        delete {
            val ids = call.receive<List<Long>>()
            val count = ruleRepository.deleteByIds(ids)
            call.respond(mapOf("deleted" to count))
        } openapi {
            summary = "Delete multiple rules"
            description = "Delete multiple rules by their IDs"
            operationId = "deleteMultipleRules"
            requestBody {
                description = "List of rule IDs to delete"
                body<List<Long>>()
            }
            response {
                HttpStatusCode.OK to {
                    description = "Number of deleted rules"
                    body<Map<String, Int>>()
                }
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))

            val rule = ruleRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "rule not found"))

            call.respond(rule)
        } openapi {
            summary = "Get rule by ID"
            description = "Retrieve a specific rule by its ID"
            operationId = "getRuleById"
            parameters {
                pathParameter<Long>("id") {
                    description = "Rule ID"
                }
            }
            response {
                HttpStatusCode.OK to {
                    description = "Rule details"
                    body<studio.tearule.api.dto.RuleResponse>()
                }
                HttpStatusCode.NotFound to {
                    description = "Rule not found"
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid ID format"
                }
            }
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
        } openapi {
            summary = "Delete rule by ID"
            description = "Delete a specific rule by its ID"
            operationId = "deleteRuleById"
            parameters {
                pathParameter<Long>("id") {
                    description = "Rule ID"
                }
            }
            response {
                HttpStatusCode.NoContent to {
                    description = "Rule deleted successfully"
                }
                HttpStatusCode.NotFound to {
                    description = "Rule not found"
                }
                HttpStatusCode.BadRequest to {
                    description = "Invalid ID format"
                }
            }
        }
    }

    get("/export/rules") {
        val rules = ruleRepository.findAll()
        call.response.headers.append("Content-Disposition", "attachment; filename=\"rules.json\"")
        call.response.headers.append("Content-Type", "application/json")
        call.respond(rules)
    } openapi {
        summary = "Export all rules"
        description = "Download all rules as a JSON file"
        operationId = "exportRules"
        response {
            HttpStatusCode.OK to {
                description = "JSON file containing all rules"
                body<List<studio.tearule.api.dto.RuleResponse>>()
            }
        }
    }

    post("/import/rules") {
        val request = call.receive<ImportRulesRequest>()
        val importedRules = request.rules.map { ruleRepository.create(it) }
        val response = ImportRulesResponse(importedRules.size, importedRules)
        call.respond(HttpStatusCode.Created, response)
    } openapi {
        summary = "Import rules"
        description = "Import multiple rules from a JSON payload"
        operationId = "importRules"
        requestBody {
            description = "Rules to import"
            body<ImportRulesRequest>()
        }
        response {
            HttpStatusCode.Created to {
                description = "Import result with created rules"
                body<ImportRulesResponse>()
            }
            HttpStatusCode.BadRequest to {
                description = "Invalid request data or DSL syntax errors"
            }
        }
    }
}
