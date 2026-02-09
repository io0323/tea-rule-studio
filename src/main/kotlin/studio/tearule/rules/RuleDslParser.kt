package studio.tearule.rules

import studio.tearule.domain.Severity

object RuleDslParser {
    private val ruleNameRegex = Regex("rule\\(\\\"(.+?)\\\"\\)")
    private val thenRegex = Regex("then\\s+(INFO|WARNING|BLOCK)")

    fun parse(dsl: String): CompiledRule {
        val name = ruleNameRegex.find(dsl)?.groupValues?.get(1)
            ?: error("dsl parse error: missing rule(\"name\")")

        val severityText = thenRegex.find(dsl)?.groupValues?.get(1)
            ?: error("dsl parse error: missing then SEVERITY")
        val severity = Severity.valueOf(severityText)

        return when {
            dsl.contains("whenMoisture") -> parseDoublePredicateRule(name, dsl, DoubleFieldCondition.Field.MOISTURE, severity)
            dsl.contains("whenPesticideLevel") -> parseDoublePredicateRule(name, dsl, DoubleFieldCondition.Field.PESTICIDE_LEVEL, severity)
            dsl.contains("whenAromaScore") -> parseIntPredicateRule(name, dsl, severity)
            else -> error("dsl parse error: unsupported condition")
        }
    }

    private fun parseDoublePredicateRule(
        name: String,
        dsl: String,
        field: DoubleFieldCondition.Field,
        severity: Severity,
    ): CompiledRule {
        val predicate = extractPredicate(dsl)
        val (op, valueText) = parseOperatorAndNumber(predicate)
        val threshold = valueText.toDoubleOrNull() ?: error("dsl parse error: threshold is not a number")

        val comparator = when (op) {
            ">" -> Comparator.GreaterThan
            ">=" -> Comparator.GreaterThanOrEqual
            "<" -> Comparator.LessThan
            "<=" -> Comparator.LessThanOrEqual
            else -> error("dsl parse error: unsupported operator: $op")
        }

        return CompiledRule(
            name = name,
            condition = DoubleFieldCondition(field, comparator, threshold),
            severity = severity,
        )
    }

    private fun parseIntPredicateRule(
        name: String,
        dsl: String,
        severity: Severity,
    ): CompiledRule {
        val predicate = extractPredicate(dsl)
        val (op, valueText) = parseOperatorAndNumber(predicate)
        val threshold = valueText.toIntOrNull() ?: error("dsl parse error: threshold is not an int")

        val comparator = when (op) {
            ">" -> IntComparator.GreaterThan
            ">=" -> IntComparator.GreaterThanOrEqual
            "<" -> IntComparator.LessThan
            "<=" -> IntComparator.LessThanOrEqual
            else -> error("dsl parse error: unsupported operator: $op")
        }

        return CompiledRule(
            name = name,
            condition = AromaScoreCondition(comparator, threshold),
            severity = severity,
        )
    }

    private fun extractPredicate(dsl: String): String {
        val braceStart = dsl.indexOf('{')
        if (braceStart < 0) error("dsl parse error: missing '{'")

        val whenIndex = dsl.indexOf("when", startIndex = 0)
        if (whenIndex < 0) error("dsl parse error: missing whenX")

        val predicateStart = dsl.indexOf('{', startIndex = whenIndex)
        val predicateEnd = dsl.indexOf('}', startIndex = predicateStart + 1)
        if (predicateStart < 0 || predicateEnd < 0 || predicateEnd <= predicateStart) {
            error("dsl parse error: missing predicate block")
        }

        return dsl.substring(predicateStart + 1, predicateEnd).trim()
    }

    private fun parseOperatorAndNumber(predicate: String): Pair<String, String> {
        val normalized = predicate.replace(" ", "")
        if (!normalized.startsWith("it")) error("dsl parse error: predicate must start with 'it'")

        val body = normalized.removePrefix("it")
        val operators = listOf(">=", "<=", ">", "<")
        val op = operators.firstOrNull { body.startsWith(it) }
            ?: error("dsl parse error: missing comparison operator")

        val number = body.removePrefix(op)
        if (number.isBlank()) error("dsl parse error: missing threshold")

        return op to number
    }
}
