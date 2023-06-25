package io.wispershadow.tech.common.el

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class SpringExpressionEvaluatorTest {
    private val expressionEvaluator = SpringExpressionEvaluator()

    @Test
    fun testGetVariables() {
        val expressionStr = "#variable1 > 100 && (T(java.lang.Math).abs(#variable2) > 1000  || T(java.lang.Math).abs(#variable2) < 2000)"
        val variables = expressionEvaluator.getVariables(expressionStr)
        Assertions.assertEquals(variables.size, 2)
        Assertions.assertTrue(variables.contains("variable1"))
        Assertions.assertTrue(variables.contains("variable2"))
    }

    @Test
    fun testEvaluateExpressionByMap() {
        val expression = "#accountNo.startsWith(\"act0\") || #paymentAmount.doubleValue() > 3000"
        val variables1 = mapOf(
            Pair("accountNo", "act001"),
            Pair("paymentAmount", BigDecimal(1000))
        )
        val result1 = expressionEvaluator.evaluateExpression(expression, variables1, Boolean::class.java)
        Assertions.assertTrue(result1!!)

        val variables2 = mapOf(
            Pair("accountNo", "act001"),
            Pair("paymentAmount", BigDecimal(3001.32))
        )
        val result2 = expressionEvaluator.evaluateExpression(expression, variables2, Boolean::class.java)
        Assertions.assertTrue(result2!!)

        val variables3 = mapOf(
            Pair("accountNo", "act101"),
            Pair("paymentAmount", BigDecimal(3000.01))
        )
        val result3 = expressionEvaluator.evaluateExpression(expression, variables3, Boolean::class.java)
        Assertions.assertTrue(result3!!)

        val variables4 = mapOf(
            Pair("accountNo", "act101"),
            Pair("paymentAmount", BigDecimal(2999.32))
        )
        val result4 = expressionEvaluator.evaluateExpression(expression, variables4, Boolean::class.java)
        Assertions.assertFalse(result4!!)
    }

    @Test
    fun testEvaluateExpressionByObj() {
        val activePayment1 = TransactionMonitorDTO().apply {
            this.payment = TransactionDTO.PaymentDTO().apply {
                this.beneficiary = Beneficiary().apply {
                    this.bankDetails = BankAccount().apply {
                        this.bankCountryCode = "CN"
                    }
                }
            }
        }

        val expression = "#restrictedCountries.contains(#root?.payment?.beneficiary?.bankDetails?.bankCountryCode?.toString())"
        val variables = mapOf(Pair("restrictedCountries", listOf("RU", "CN")))
        val result1 = expressionEvaluator.evaluateExpression(expression, activePayment1, variables, Boolean::class.java)
        Assertions.assertTrue(result1!!)
        val activePayment2 = TransactionMonitorDTO()
        val result2 = expressionEvaluator.evaluateExpression(expression, activePayment2, variables, Boolean::class.java)
        Assertions.assertFalse(result2!!)
    }

    @Test
    fun testEvaluateExpressionByMapMultiThread() {
        val expression = "#accountNo.startsWith(\"act0\") || #paymentAmount.doubleValue() > 3000"
        val executor = Executors.newFixedThreadPool(4)
        val resultMap = ConcurrentHashMap<String, Boolean>()
        val completableFutureArr = arrayOf(CompletableFuture.runAsync(Runnable {
            val result = expressionEvaluator.evaluateExpression(
                expression, mapOf(
                    Pair("accountNo", "act001"),
                    Pair("paymentAmount", BigDecimal(1000))
                ), Boolean::class.java
            )
            resultMap["1"] = result!!
        }, executor), CompletableFuture.runAsync(Runnable {
            val result = expressionEvaluator.evaluateExpression(
                expression, mapOf(
                    Pair("accountNo", "act001"),
                    Pair("paymentAmount", BigDecimal(3001.31))
                ), Boolean::class.java
            )
            resultMap["2"] = result!!
        }, executor), CompletableFuture.runAsync(Runnable {
            val result = expressionEvaluator.evaluateExpression(
                expression, mapOf(
                    Pair("accountNo", "act101"),
                    Pair("paymentAmount", BigDecimal(3000.01))
                ), Boolean::class.java
            )
            resultMap["3"] = result!!
        }, executor), CompletableFuture.runAsync(Runnable {
            val result = expressionEvaluator.evaluateExpression(
                expression, mapOf(
                    Pair("accountNo", "act101"),
                    Pair("paymentAmount", BigDecimal(2999.32))
                ), Boolean::class.java
            )
            resultMap["4"] = result!!
        }, executor))
        CompletableFuture.allOf(*completableFutureArr).thenRun {
            Assertions.assertEquals(resultMap.size, 4)
            Assertions.assertTrue(resultMap["1"]!!)
            Assertions.assertTrue(resultMap["2"]!!)
            Assertions.assertTrue(resultMap["3"]!!)
            Assertions.assertFalse(resultMap["4"]!!)
        }
    }
}