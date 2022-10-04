package io.wispershadow.tech.common.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

object CompareUtils {
    fun compareObjects(source: Any?, target: Any?, maxPrecision: Int? = null): Int {
        if (source == null) {
            throw NonComparableException("Source object is null")
        }
        if (target == null) {
            throw NonComparableException("Target object is null")
        }
        if (source is Number && target is Number) {
            return compareNumber(source, target, maxPrecision)
        }
        else if (source::class.java == target::class.java) {
            if (source is Comparable<*>) {
                return (source as Comparable<Any>).compareTo(target as Comparable<Any>)
            }
            else {
                throw NonComparableException("Item is not comparable: ${source::class.java}")
            }
        }
        else {
            throw NonComparableException("Source object class ${source::class.java} and target object class ${target::class.java} does not match")
        }
    }

    private fun compareNumber(source: Number, target: Number, maxPrecision: Int?): Int {
        val sourceDecimal = if (maxPrecision == null) {
            BigDecimal.valueOf(source.toDouble())
        }
        else {
            BigDecimal.valueOf(source.toDouble()).setScale(maxPrecision, RoundingMode.HALF_UP)
        }
        val targetDecimal = if (maxPrecision == null) {
            BigDecimal.valueOf(target.toDouble())
        }
        else {
            BigDecimal.valueOf(target.toDouble()).setScale(maxPrecision, RoundingMode.HALF_UP)
        }
        return sourceDecimal.compareTo(targetDecimal)
    }
}

class NonComparableException(message: String): RuntimeException(message)