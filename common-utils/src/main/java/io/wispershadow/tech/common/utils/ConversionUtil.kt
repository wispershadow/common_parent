package io.wispershadow.tech.common.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object ConversionUtil {
    private val dateTimeFormatters: MutableMap<String, DateTimeFormatter> = mutableMapOf()

    /**
     * convert object to Number.
     * @param originalObj value
     * @return number
     */
    fun convertObjToNumber(originalObj: Any?): Number? {
        return when (originalObj) {
            null -> null
            is Number -> originalObj
            is String -> originalObj.toDouble()
            else -> throw RuntimeException("Unable to convert " + originalObj.javaClass + " to number")
        }
    }

    /**
     * convert object to int.
     * @param originalObj original object
     * @return integer
     */
    fun convertObjToInt(originalObj: Any?): Int? {
        return when (originalObj) {
            null -> null
            is Int -> originalObj
            is String -> originalObj.toInt()
            is Number -> originalObj.toInt()
            else -> throw RuntimeException("Unable to convert " + originalObj.javaClass + " to int")
        }
    }

    /**
     * convert object to long.
     * @param originalObj original object
     * @return Long
     */
    fun convertObjToLong(originalObj: Any?): Long? {
        return when (originalObj) {
            null -> null
            is Long -> originalObj
            is String -> originalObj.toLong()
            is Number -> originalObj.toLong()
            else -> throw RuntimeException("Unable to convert " + originalObj.javaClass + " to long")
        }
    }

    /**
     * convert object to float.
     * @param originalObj original object
     * @return float
     */
    fun convertObjToFloat(originalObj: Any?): Float? {
        return when (originalObj) {
            null -> null
            is Float -> originalObj
            is String -> originalObj.toFloat()
            is Number -> originalObj.toFloat()
            else -> throw RuntimeException("Unable to convert " + originalObj.javaClass + " to float")
        }
    }

    /**
     * convert object to double.
     * @param originalObj original object
     * @return double
     */
    fun convertObjToDouble(originalObj: Any?): Double? {
        return when (originalObj) {
            null -> null
            is Double -> originalObj
            is String -> originalObj.toDouble()
            is Number -> originalObj.toDouble()
            else -> throw RuntimeException("Unable to convert " + originalObj.javaClass + " to double")
        }
    }


    fun convertObjToDecimal(originalObj: Any?): BigDecimal? {
        return when (originalObj) {
            null -> null
            is BigDecimal -> originalObj
            is String -> originalObj.toBigDecimal()
            is Number -> BigDecimal(originalObj.toDouble())
            else -> throw RuntimeException("Unable to convert " + originalObj.javaClass + " to BigDecimal")
        }
    }

    fun convertObjToDecimalF(originalObj: Any?, fraction: Int): BigDecimal? {
        return originalObj?.let {
            convertObjToDecimal(originalObj)?.setScale(fraction, RoundingMode.HALF_UP)
        }
    }

    /**
     * convert to date time. only allowed date/long/string.
     * @param datePropVal original object
     * @param dateFormat date format
     * @return Date as milliseconds
     */
    fun convertObjToTimeMillisF(datePropVal: Any?, dateFormat: String): Long? {
        return when (datePropVal) {
            null -> null
            is Date -> datePropVal.time
            is Timestamp -> datePropVal.time
            is LocalDateTime -> datePropVal.toInstant(ZoneOffset.UTC).toEpochMilli()
            is LocalDate -> datePropVal.toEpochDay() * 24 * 60 * 60 * 1000
            is Instant -> datePropVal.toEpochMilli()
            is Long -> datePropVal
            is String -> {
                val convertedDate = convertStringToDateF(datePropVal, dateFormat)
                convertedDate?.time
            }
            else -> throw RuntimeException("Unable to convert " + datePropVal.javaClass + " to DateTime")
        }
    }



    fun convertStringToDateF(value: String?, dateTimeFormat: String): Date? {
        if (value == null) {
            return null
        }
        synchronized(dateTimeFormatters) {
            if (!dateTimeFormatters.containsKey(dateTimeFormat)) {
                dateTimeFormatters[dateTimeFormat] = DateTimeFormatter.ofPattern(dateTimeFormat)
            }
        }
        val localDateTime = LocalDateTime.parse(value, dateTimeFormatters.getValue(dateTimeFormat))
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC))
    }



    private val hexArray = "0123456789ABCDEF".toCharArray()

    fun byteArrayToHexString(arr: ByteArray): String {
        val hexChars = CharArray(arr.size * 2)
        arr.forEachIndexed { index, value ->
            val v = value.toInt() and 0xFF
            hexChars[index * 2] = hexArray[v ushr 4]
            hexChars[index * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun hexStringToByteArray(value: String): ByteArray {
        val len = value.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(value[i], 16) shl 4) + Character.digit(value[i + 1], 16)).toByte()
        }
        return data
    }
}