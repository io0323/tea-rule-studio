package studio.tearule.rules

import studio.tearule.domain.Severity

@DslMarker
annotation class TeaRuleDsl

interface Condition {
    fun evaluate(subject: TeaLotSnapshot): Boolean
}

data class TeaLotSnapshot(
    val moisture: Double,
    val pesticideLevel: Double,
    val aromaScore: Int,
)

sealed interface Comparator<T> {
    fun test(left: T, right: T): Boolean

    data object GreaterThan : Comparator<Double> {
        override fun test(left: Double, right: Double): Boolean = left > right
    }

    data object GreaterThanOrEqual : Comparator<Double> {
        override fun test(left: Double, right: Double): Boolean = left >= right
    }

    data object LessThan : Comparator<Double> {
        override fun test(left: Double, right: Double): Boolean = left < right
    }

    data object LessThanOrEqual : Comparator<Double> {
        override fun test(left: Double, right: Double): Boolean = left <= right
    }
}

data class DoubleFieldCondition(
    val field: Field,
    val comparator: Comparator<Double>,
    val threshold: Double,
) : Condition {
    enum class Field {
        MOISTURE,
        PESTICIDE_LEVEL,
    }

    override fun evaluate(subject: TeaLotSnapshot): Boolean {
        val value = when (field) {
            Field.MOISTURE -> subject.moisture
            Field.PESTICIDE_LEVEL -> subject.pesticideLevel
        }
        return comparator.test(value, threshold)
    }
}

data class AromaScoreCondition(
    val comparator: IntComparator,
    val threshold: Int,
) : Condition {
    override fun evaluate(subject: TeaLotSnapshot): Boolean = comparator.test(subject.aromaScore, threshold)
}

sealed interface IntComparator {
    fun test(left: Int, right: Int): Boolean

    data object GreaterThan : IntComparator {
        override fun test(left: Int, right: Int): Boolean = left > right
    }

    data object GreaterThanOrEqual : IntComparator {
        override fun test(left: Int, right: Int): Boolean = left >= right
    }

    data object LessThan : IntComparator {
        override fun test(left: Int, right: Int): Boolean = left < right
    }

    data object LessThanOrEqual : IntComparator {
        override fun test(left: Int, right: Int): Boolean = left <= right
    }
}

data class CompiledRule(
    val name: String,
    val condition: Condition,
    val severity: Severity,
) {
    fun evaluate(subject: TeaLotSnapshot): RuleEvaluation =
        if (condition.evaluate(subject)) {
            RuleEvaluation.Fail(
                message = "$name failed",
                severity = severity,
            )
        } else {
            RuleEvaluation.Pass
        }
}

sealed interface RuleEvaluation {
    data object Pass : RuleEvaluation

    data class Fail(
        val message: String,
        val severity: Severity,
    ) : RuleEvaluation
}

@TeaRuleDsl
class RuleBuilder internal constructor(
    private val name: String,
) {
    private var compiled: CompiledRule? = null

    fun whenMoisture(predicate: (Double) -> Boolean): ThenBuilder {
        val condition = object : Condition {
            override fun evaluate(subject: TeaLotSnapshot): Boolean = predicate(subject.moisture)
        }
        return ThenBuilder(name, condition)
    }

    fun whenPesticideLevel(predicate: (Double) -> Boolean): ThenBuilder {
        val condition = object : Condition {
            override fun evaluate(subject: TeaLotSnapshot): Boolean = predicate(subject.pesticideLevel)
        }
        return ThenBuilder(name, condition)
    }

    fun whenAromaScore(predicate: (Int) -> Boolean): ThenBuilder {
        val condition = object : Condition {
            override fun evaluate(subject: TeaLotSnapshot): Boolean = predicate(subject.aromaScore)
        }
        return ThenBuilder(name, condition)
    }

    @TeaRuleDsl
    inner class ThenBuilder internal constructor(
        private val name: String,
        private val condition: Condition,
    ) {
        infix fun then(severity: Severity) {
            this@RuleBuilder.compiled = CompiledRule(name = name, condition = condition, severity = severity)
        }
    }

    internal fun build(): CompiledRule =
        requireNotNull(compiled) { "rule(\"$name\") is missing 'then SEVERITY'" }
}

fun rule(name: String, block: RuleBuilder.() -> Unit): CompiledRule {
    val builder = RuleBuilder(name)
    builder.block()
    return builder.build()
}
