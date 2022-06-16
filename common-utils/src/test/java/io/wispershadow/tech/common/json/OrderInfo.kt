package io.wispershadow.tech.common.json

import com.fasterxml.jackson.databind.annotation.JsonDeserialize

class OrderInfo {
    lateinit var orderId: String

    @JsonDeserialize(using = KeepAsJsonDeserializer::class)
    lateinit var paymentInfo: String
}