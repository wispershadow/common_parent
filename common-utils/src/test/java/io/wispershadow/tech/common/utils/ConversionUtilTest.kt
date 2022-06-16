package io.wispershadow.tech.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class ConversionUtilTest {
    @Test
    fun testByteArrayConversion() {
        val sourceStr = "Test String ${Math.random()}"
        val byteArray = sourceStr.toByteArray(Charsets.UTF_8)
        val hexString = ConversionUtil.byteArrayToHexString(byteArray)
        val convertedByteArray = ConversionUtil.hexStringToByteArray(hexString)
        val convertedStr = String(convertedByteArray)
        Assertions.assertEquals(sourceStr, convertedStr)
    }

    @Test
    fun testConvertObjToTimeMillisF() {
        val timeLong = ConversionUtil.convertObjToTimeMillisF(LocalDate.now(), "yyyy/MM/dd")
        val result = Date(timeLong!!)
        println(result)
    }
}