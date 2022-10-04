package io.wispershadow.tech.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class ConversionUtilsTest {
    @Test
    fun testByteArrayConversion() {
        val sourceStr = "Test String ${Math.random()}"
        val byteArray = sourceStr.toByteArray(Charsets.UTF_8)
        val hexString = ConversionUtils.byteArrayToHexString(byteArray)
        val convertedByteArray = ConversionUtils.hexStringToByteArray(hexString)
        val convertedStr = String(convertedByteArray)
        Assertions.assertEquals(sourceStr, convertedStr)
    }

    @Test
    fun testConvertObjToTimeMillisF() {
        val timeLong = ConversionUtils.convertObjToTimeMillisF(LocalDate.now(), "yyyy/MM/dd")
        val result = Date(timeLong!!)
        println(result)
    }
}