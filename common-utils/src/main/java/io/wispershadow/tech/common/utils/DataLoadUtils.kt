package io.wispershadow.tech.common.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.stream.Collectors

object DataLoadUtils {
    private val objectMapper = ObjectMapper()

    init {
        objectMapper.apply {
            this.dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            this.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            this.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        }
    }

    @Synchronized
    fun configure(): DataLoadUtils {
        return this
    }

    fun loadTestData(fileName: String): String {
        val inputStream = DataLoadUtils::class.java.classLoader.getResourceAsStream(fileName)
        return String(inputStream.readBytes(), Charsets.UTF_8)
    }

    fun loadTestDataAsLines(fileName: String): List<String> {
        val inputStream = DataLoadUtils::class.java.classLoader.getResourceAsStream(fileName)
        return BufferedReader(InputStreamReader(inputStream)).lines().collect(Collectors.toList()) as List<String>
    }

    fun <T> loadTestDataAsPojo(fileName: String, targetClass: Class<T>): T {
        val inputStream = DataLoadUtils::class.java.classLoader.getResourceAsStream(fileName)
        return objectMapper.readValue(inputStream, targetClass)
    }

    fun <T> loadTestDataAsPojoList(fileName: String, targetClass: Class<T>): List<T> {
        val inputStream = DataLoadUtils::class.java.classLoader.getResourceAsStream(fileName)
        val collectionType = objectMapper.typeFactory.constructCollectionType(List::class.java, targetClass)
        return objectMapper.readValue(inputStream, collectionType)
    }
}