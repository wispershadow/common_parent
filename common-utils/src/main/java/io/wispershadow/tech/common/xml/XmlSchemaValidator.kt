@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package io.wispershadow.tech.common.xml

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.w3c.dom.ls.LSResourceResolver
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.*
import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

class XmlSchemaValidator(val schemaLocation: String = "xsd") {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(XmlSchemaValidator::class.java)
    }

    private val schema: Schema

    init {
        schema = buildSchema()
    }


    fun validate(xmlData: String) {
        val validator = schema.newValidator()
        validator.validate(StreamSource(ByteArrayInputStream(xmlData.toByteArray(Charsets.UTF_8))))
    }


    private fun buildSchema(): Schema {
        val resolver = PathMatchingResourcePatternResolver()
        val resources = resolver.getResources("classpath*:${schemaLocation}/*.xsd")
        val streamResourceArray: Array<Source> = resources.map { resource ->
            val xsdFileName = resource.filename
            logger.debug("Loading xsd resource: {}", xsdFileName)
            StreamSource(resource.inputStream, xsdFileName)
        }.toTypedArray()
        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        schemaFactory.resourceResolver = LSResourceResolver { type, namespaceURI, publicId, systemId, baseURI ->
            val input = DOMInputImpl().apply {
                this.publicId = publicId
                this.systemId = systemId
                this.baseURI = baseURI
                this.characterStream = InputStreamReader(getSchemaAsStream(systemId, baseURI, "${schemaLocation}/"))
            }
            input
        }
        return schemaFactory.newSchema(streamResourceArray)
    }

    private fun getSchemaAsStream(systemId: String, baseUri: String, localPath: String): InputStream? {
        val inputStream = getSchemaFromClasspath(systemId, localPath)
        if (inputStream != null) {
            return inputStream
        }
        return getSchemaFromWeb(baseUri, systemId)
    }

    private fun getSchemaFromClasspath(systemId: String, localPath: String): InputStream {
        val resourceFullPath = "${localPath}${systemId}"
        return XmlSchemaValidator::class.java.classLoader.getResourceAsStream(resourceFullPath)
    }

    private fun getSchemaFromWeb(baseUri: String, systemId: String): InputStream? {
        try {
            val uri = URI(systemId)
            if (uri.isAbsolute) {
                return urlToInputStream(uri.toURL(), "text/xml")
            }
            return getSchemaRelativeToBaseUri(baseUri, systemId)
        } catch (e: Exception) {
            logger.error("Error getting schema from web", e)
        }
        return null
    }

    private fun urlToInputStream(url: URL, accept: String): InputStream? {
        var con: HttpURLConnection? = null
        var inputStream: InputStream? = null
        return try {
            con = url.openConnection() as HttpURLConnection
            con.connectTimeout = 15000
            con.setRequestProperty("User-Agent", "Name of my application.")
            con.readTimeout = 15000
            con.setRequestProperty("Accept", accept)
            con.connect()
            val responseCode: Int = con.getResponseCode()
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == 307 || responseCode == 303) {
                val redirectUrl: String = con.getHeaderField("Location")
                return try {
                    val newUrl = URL(redirectUrl)
                    urlToInputStream(newUrl, accept)
                } catch (e: MalformedURLException) {
                    val newUrl = URL(url.protocol + "://" + url.host + redirectUrl)
                    urlToInputStream(newUrl, accept)
                }
            }
            inputStream = con.inputStream
            inputStream
        } catch (e: SocketTimeoutException) {
            throw RuntimeException(e)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun getSchemaRelativeToBaseUri(baseUri: String, systemId: String): InputStream? {
        return try {
            val url = URL(baseUri + systemId)
            urlToInputStream(url, "text/xml")
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}