package io.wispershadow.tech.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CompareUtilsTest {

    @Test
    fun testCompareTemporal1() {
        val date1 = LocalDate.now()
        val date2 = date1.plus(1, ChronoUnit.DAYS)
        val compareRes = CompareUtils.compareObjects(date2, date1)
        Assertions.assertTrue(compareRes > 0)
    }

    @Test
    fun testCompareTemporal2() {
        try {
            val date1 = LocalDateTime.now()
            val date2 = ZonedDateTime.now()
            CompareUtils.compareObjects(date1, date2)
            Assertions.fail<String>()
        }
        catch (e: NonComparableException) {
        }
    }

    @Test
    fun testCompareTemporal3() {

    }

    @Test
    fun testCompareNumber1() {
        val number1 = 979.94f
        val number2 = 980L
        val compareRes = CompareUtils.compareObjects(number1, number2)
        Assertions.assertTrue(compareRes < 0)
    }

    @Test
    fun testCompareNumber2() {
        val number1 = 999.991
        val number2 = 999.992
        val compareRes1 = CompareUtils.compareObjects(number1, number2, 2)
        Assertions.assertEquals(compareRes1, 0)
        val compareRes2 = CompareUtils.compareObjects(number1, number2)
        Assertions.assertEquals(compareRes2, -1)
    }
}