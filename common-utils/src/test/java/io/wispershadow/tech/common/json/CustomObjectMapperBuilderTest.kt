package io.wispershadow.tech.common.json

import io.wispershadow.tech.common.config.JsonPropertiesInclusionConfig
import io.wispershadow.tech.common.config.JsonSerializeConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CustomObjectMapperBuilderTest {

    @Test
    fun testSerializeIncludeProperty1() {
        val jsonSerializeConfig = JsonSerializeConfig().apply {
            this.propertiesInclusion = listOf(JsonPropertiesInclusionConfig().apply {
                this.className = OrderInfo::class.java.name
                this.propertyNames = listOf("paymentInfo", "fakeProperty")
            })
        }
        val builder = CustomObjectMapperBuilder(jsonSerializeConfig, null)
        val objectMapper = builder.build()
        val orderInfo = OrderInfo().apply {
            this.orderId = "123"
            this.paymentInfo = "paymentInfo"
        }
        val serializeResult = objectMapper.writeValueAsString(orderInfo)
        Assertions.assertEquals(serializeResult, "{\"paymentInfo\":\"paymentInfo\"}")
    }

    @Test
    fun testSerializeIncludeProperty2() {
        val jsonSerializeConfig = JsonSerializeConfig().apply {
            this.propertiesInclusion = listOf(JsonPropertiesInclusionConfig().apply {
                this.className = OrderInfo::class.java.name
                this.propertyNames = listOf("paymentInfo", "fakeProperty")
            })
        }
        val builder = CustomObjectMapperBuilder(jsonSerializeConfig, null)
        val objectMapper = builder.build()
        val discountInfo = DiscountInfo().apply {
            this.discountId = "13411"
            this.discountType = "PERCENTAGE"
        }
        val serializeResult = objectMapper.writeValueAsString(discountInfo)
        Assertions.assertEquals(serializeResult, "{\"discountId\":\"13411\",\"discountType\":\"PERCENTAGE\"}")
    }

    @Test
    fun testSerializeExcludeProperty1() {
        val jsonSerializeConfig = JsonSerializeConfig().apply {
            this.propertiesInclusion = listOf(JsonPropertiesInclusionConfig().apply {
                this.className = OrderInfo::class.java.name
                this.propertyNames = listOf("paymentInfo", "fakeProperty")
                this.inclusionType = JsonPropertiesInclusionConfig.TYPE_EXCLUDE
            })
        }
        val builder = CustomObjectMapperBuilder(jsonSerializeConfig, null)
        val objectMapper = builder.build()
        val orderInfo = OrderInfo().apply {
            this.orderId = "123"
            this.paymentInfo = "paymentInfo"
        }
        val serializeResult = objectMapper.writeValueAsString(orderInfo)
        Assertions.assertEquals(serializeResult, "{\"orderId\":\"123\"}")
    }

    @Test
    fun testSerializeExcludeProperty2() {
        val jsonSerializeConfig = JsonSerializeConfig().apply {
            this.propertiesInclusion = listOf(JsonPropertiesInclusionConfig().apply {
                this.className = OrderInfo::class.java.name
                this.propertyNames = listOf("paymentInfo", "fakeProperty")
                this.inclusionType = JsonPropertiesInclusionConfig.TYPE_EXCLUDE
            })
        }
        val builder = CustomObjectMapperBuilder(jsonSerializeConfig, null)
        val objectMapper = builder.build()
        val discountInfo = DiscountInfo().apply {
            this.discountId = "13411"
            this.discountType = "PERCENTAGE"
        }
        val serializeResult = objectMapper.writeValueAsString(discountInfo)
        Assertions.assertEquals(serializeResult, "{\"discountId\":\"13411\",\"discountType\":\"PERCENTAGE\"}")
    }
}