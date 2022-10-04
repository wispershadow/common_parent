package io.wispershadow.tech.common.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KeepAsJsonDeserializerTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun testDeserialize() {
        val inputStream = KeepAsJsonDeserializerTest::class.java.classLoader.getResourceAsStream("json/complex.json")
        val orderInfo = objectMapper.readValue(inputStream, OrderInfo::class.java)
        Assertions.assertEquals(orderInfo.paymentInfo, "{\"paymentMethod\":\"wechat\",\"amount:\":31.24}")
    }

    @Test
    fun testSerailize() {
        val objectMapper = CustomObjectMapperBuilder().build()
        val orderInfo = OrderInfo().apply {
            this.orderId = "abcd"
            this.paymentInfo = "whatever"
        }
        println(objectMapper.writeValueAsString(orderInfo))
    }
}