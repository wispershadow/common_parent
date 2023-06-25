package io.wispershadow.tech.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.PropertyAccessorFactory
import java.math.BigDecimal

class PropertyUtilsTest {

    @Test
    fun testGetSetPropertyNormalBean() {
        val paymentTransaction = PaymentTransaction()
        val payerInfo = CustomerInfo()
        val benificiaryInfo = CustomerInfo()
        paymentTransaction.apply {
            this.payer = payerInfo
            this.beneficiary = benificiaryInfo
            this.paymentMethods = listOf(
                PaymentMethod().apply {
                    this.paymentMethodName = "wechat"
                }
            )
        }
        PropertyUtils.setPropertyValue(paymentTransaction, "amount", BigDecimal.valueOf(125.12))
        PropertyUtils.setPropertyValue(paymentTransaction, "payer.country", "CN")
        PropertyUtils.setPropertyValue(paymentTransaction, "paymentMethods[0].amount", BigDecimal.valueOf(100.00))
        val amount = PropertyUtils.getPropertyValue(paymentTransaction, "amount")
        val payerCountry = PropertyUtils.getPropertyValue(paymentTransaction, "payer.country")
        val paymentAmount = PropertyUtils.getPropertyValue(paymentTransaction, "paymentMethods[0].amount")
        Assertions.assertEquals(amount, BigDecimal.valueOf(125.12))
        Assertions.assertEquals(payerCountry, "CN")
        Assertions.assertEquals(paymentAmount, BigDecimal.valueOf(100.00))
    }

    @Test
    fun testGetSetPropertyMap1() {
        val dataMap = mutableMapOf("transactionId" to "124111")
        PropertyUtils.setPropertyValue(dataMap, "amount", BigDecimal.valueOf(125.12))
        val amount = PropertyUtils.getPropertyValue(dataMap, "amount")
        Assertions.assertEquals(amount, BigDecimal.valueOf(125.12))
    }

    @Test
    fun testGetSetPropertyMap2() {
        val dataMap = mutableMapOf("transactionId" to "124111",
            "payer" to mutableMapOf("country" to "CN"),
            "paymentMethods" to listOf(
                PaymentMethod().apply {
                    this.paymentMethodName = "wechat"
                }
            ))
        val payerCountry = PropertyUtils.getPropertyValue(dataMap, "payer.country")
        Assertions.assertEquals(payerCountry, "CN")
        val paymentMethodName = PropertyUtils.getPropertyValue(dataMap, "paymentMethods[0].paymentMethodName")
        Assertions.assertEquals(paymentMethodName, "wechat")
    }

    @Test
    fun testGetSetPropertyBeanWrapper() {
        val paymentTransaction = PaymentTransaction()
        val payerInfo = CustomerInfo()
        val benificiaryInfo = CustomerInfo()
        paymentTransaction.apply {
            this.payer = payerInfo
            this.beneficiary = benificiaryInfo
            this.paymentMethods = listOf(
                PaymentMethod().apply {
                    this.paymentMethodName = "wechat"
                }
            )
        }
        val paymentTransactionWrapper = PropertyAccessorFactory.forBeanPropertyAccess(paymentTransaction)
        PropertyUtils.setPropertyValue(paymentTransactionWrapper, "amount", BigDecimal.valueOf(125.12))
        PropertyUtils.setPropertyValue(paymentTransactionWrapper, "payer.country", "CN")
        PropertyUtils.setPropertyValue(paymentTransactionWrapper, "paymentMethods[0].amount", BigDecimal.valueOf(100.00))
        val amount = PropertyUtils.getPropertyValue(paymentTransactionWrapper, "amount")
        val payerCountry = PropertyUtils.getPropertyValue(paymentTransactionWrapper, "payer.country")
        val paymentAmount = PropertyUtils.getPropertyValue(paymentTransactionWrapper, "paymentMethods[0].amount")
        Assertions.assertEquals(amount, BigDecimal.valueOf(125.12))
        Assertions.assertEquals(payerCountry, "CN")
        Assertions.assertEquals(paymentAmount, BigDecimal.valueOf(100.00))
    }

    class PaymentTransaction {
        lateinit var transactionId: String
        lateinit var amount: BigDecimal
        lateinit var country: String
        lateinit var payer: CustomerInfo
        lateinit var beneficiary: CustomerInfo
        var paymentMethods = emptyList<PaymentMethod>()
    }

    class CustomerInfo {
        lateinit var country: String
        lateinit var lastName: String
        lateinit var firstName: String
        var midName: String? = null
        var address: String? = null
        var phoneNumber: String? = null
    }

    class PaymentMethod {
        lateinit var paymentMethodName: String
        lateinit var amount: BigDecimal
    }
}