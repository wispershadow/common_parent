package io.wispershadow.tech.common.json

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*
import java.util.regex.Pattern

class JsonAttributeValueExtractor(val attributeNames: List<String>) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(JsonAttributeValueExtractor::class.java)
    }

    private val attributeSearchPattern = Pattern.compile(".+\"(${attributeNames.joinToString("|")})\":\\s*\"([^\"\"]+)\".+", Pattern.DOTALL)

    fun extractAttribute(requestData: ByteArray): Optional<String> {
        return doExtractAttribute(Scanner(ByteArrayInputStream(requestData)))
    }

    fun extractAttribute(requestStream: InputStream): Optional<String> {
        return doExtractAttribute(Scanner(requestStream))
    }

    private fun doExtractAttribute(scanner: Scanner): Optional<String> {
        val resultList = mutableListOf<String>()
        try {
            while (scanner.hasNextLine()) {
                val nextLine = scanner.nextLine()
                val matcher = attributeSearchPattern.matcher(nextLine)
                if (matcher.matches()) {
                    resultList.add(matcher.group(2))
                    break
                }
            }
        }
        catch (e: Exception) {

        }
        finally {
            scanner.close()
        }
        return if (resultList.isEmpty()) {
            Optional.empty()
        }
        else {
            Optional.of(resultList[0])
        }
    }

}