package io.wispershadow.tech.common.reverseproxy.test

import io.wispershadow.tech.common.reverseproxy.ReverseProxyUtils
import io.wispershadow.tech.common.reverseproxy.config.BootConfigWebMvc
import io.wispershadow.tech.common.testbackend.config.BootConfigWebFlux
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

class ProxyTest {
    private val restTemplate = RestTemplate()

    companion object {
        private lateinit var applContextProxy: ConfigurableApplicationContext
        private val logger = LoggerFactory.getLogger(ProxyTest::class.java)

        private lateinit var applContextBackend: ConfigurableApplicationContext

        private var portProxy: Int = 0
        private var portBackend: Int = 0

        @BeforeAll
        @JvmStatic
        fun setUpApplicationContext() {
            applContextProxy = SpringApplicationBuilder(BootConfigWebMvc::class.java)
                .web(WebApplicationType.SERVLET)
                .profiles("proxy")
                .properties("spring.main.allow-bean-definition-overriding=true")
                .run()
            portProxy = applContextProxy.environment.getProperty("local.server.port", Int::class.java, 8080)

            applContextBackend = SpringApplicationBuilder(BootConfigWebFlux::class.java)
                .web(WebApplicationType.REACTIVE)
                .profiles("backend")
                .run()
            portBackend = applContextBackend.environment.getProperty("local.server.port", Int::class.java, 9999)
        }
    }

    @Test
    fun testProxyPutJson() {
        restTemplate.put(
            "http://localhost:$portProxy/base/proxy/json",
            mapOf("key1" to "value1", "key2" to "value2"),
            Map::class.java
        )
    }

    @Test
    fun testProxyGetJson() {
        val getResponse = restTemplate.getForEntity("http://localhost:$portProxy/base/proxy/json", Map::class.java)
        val expectedResponse = mapOf("key1" to "value1", "key2" to "value2")
        Assertions.assertEquals(getResponse.body, expectedResponse)
    }

    @Test
    fun testPostMultipartForm() {
        val partsMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.MULTIPART_FORM_DATA
        }

        val fileResource: Resource = ClassPathResource("testfile.txt")
        partsMap.add("testfile", fileResource)

        partsMap.add("key1", "value1")
        partsMap.add("key2", "value2")
        val requestEntity = HttpEntity(partsMap, headers)
        val response: String? = restTemplate.postForObject("http://localhost:$portProxy/base/proxy/multipartupload", requestEntity, String::class.java)
        Assertions.assertEquals(response, "OK")
    }

    @Test
    fun testUploadMultipleFiles() {
        val partsMap: MultiValueMap<String, Any> = LinkedMultiValueMap()
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.MULTIPART_FORM_DATA
        }

        val fileResource1: Resource = ClassPathResource("testfile.txt")
        partsMap.add("files", fileResource1)
        val fileResource2: Resource = ClassPathResource("testfile2.json")
        partsMap.add("files", fileResource2)
        val requestEntity = HttpEntity(partsMap, headers)
        val response: String? = restTemplate.postForObject("http://localhost:$portProxy/base/proxy/upload", requestEntity, String::class.java)
        Assertions.assertEquals(response, "OK")
    }


    @Test
    fun testPostForm() {
        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_FORM_URLENCODED
            this.add("Cookie", "name1=value1; name2=value2")
        }
        formData.add("key1", "value1")
        formData.add("key2", "value2")
        formData.add("key3", "value3")
        val requestEntity = HttpEntity(formData, headers)
        val response: String? = restTemplate.postForObject("http://localhost:$portProxy/base/proxy/form", requestEntity, String::class.java)
        Assertions.assertEquals(response, "OK")
    }

    @Test
    fun testOneHopHeader() {
        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        val oneHopHeaders = ReverseProxyUtils.hopByHopHeaders
        val randomHeader = oneHopHeaders.random()
        val headerValue = if ("transfer-encoding" == randomHeader) {
            "gzip"
        }
        else {
            "testValue"
        }
        logger.info("Random one-hop header selected: {}", randomHeader)
        val headers = HttpHeaders().apply {
            this.contentType = MediaType.APPLICATION_FORM_URLENCODED
            this.add(randomHeader, headerValue)
        }
        formData.add("key1", "value1")
        val requestEntity = HttpEntity(formData, headers)
        val response = restTemplate.postForEntity("http://localhost:$portProxy/base/proxy/onehopcheck", requestEntity, ByteArray::class.java)
        Assertions.assertEquals(response.statusCode, HttpStatus.OK)
    }

    @Test
    fun testNotModified() {
        val response: ResponseEntity<Void> = restTemplate.exchange(
            "http://localhost:$portProxy/base/proxy/notModified",
            HttpMethod.GET,
            null,
            Void::class.java
        )
        Assertions.assertEquals(response.statusCode, HttpStatus.NOT_MODIFIED)
    }

    @Test
    fun testDownLoadFileSuccess() {
        val fileName = "testfile2.json"
        val response: ResponseEntity<Resource> = restTemplate.exchange(
            "http://localhost:$portProxy/base/proxy/download/${fileName}",
            HttpMethod.GET,
            HttpEntity<Void>(null, HttpHeaders()),
            Resource::class.java
        )

        Assertions.assertEquals(response.statusCode, HttpStatus.OK)
        response.body?.let { resource ->
            Assertions.assertTrue(resource.exists())
            Assertions.assertEquals(fileName, resource.filename)
            val fileContent = String(resource.inputStream.readAllBytes(), Charsets.UTF_8)
            logger.info("File content is: {}", fileContent)
        } ?: run {
            Assertions.fail("Response body is null")
        }
    }

    @Test
    fun testDownLoadFileNotFound() {
        val fileName = "unknown.json"
        try {
            val response: ResponseEntity<Resource> = restTemplate.exchange(
                "http://localhost:$portProxy/base/proxy/download/${fileName}",
                HttpMethod.GET,
                HttpEntity<Void>(null, HttpHeaders()),
                Resource::class.java
            )
        }
        catch (e: Exception) {
            Assertions.assertTrue(e is HttpServerErrorException)
        }

    }

    @Test
    fun testProxyRequestTimeout() {
        try {
            restTemplate.getForEntity(
                "http://localhost:$portProxy/base/proxy/timeout",
                String::class.java
            )
            Assertions.fail()
        }
        catch (e: Exception) {
            Assertions.assertTrue(e is HttpServerErrorException)
        }
    }


    @Test
    fun testResponseCookieRetainSameSiteStrict() {
        val requestEntity = HttpEntity(null, null)
        restTemplate.postForEntity(            "http://localhost:$portProxy/base/proxy/cookie?caseId=1",
            requestEntity,
            String::class.java
        ).also { response ->
            checkCookieMatch(response)
        }
    }

    private fun checkCookieMatch(response: ResponseEntity<*>) {
        Assertions.assertTrue(response.headers.containsKey("Set-Cookie"))
        val cookieValues = mutableMapOf<String, String>()
        response.headers["Set-Cookie"]?.forEach {
            val parts = it.split(";")
            parts.forEachIndexed {index, part ->
                val cookieParts = part.split("=")
                if (index == 0 && cookieParts.size == 2) {
                    cookieValues[cookieParts[0]] = cookieParts[1]
                }
                else if (cookieParts[0] == "Path") {
                    Assertions.assertTrue(cookieParts[1].startsWith("/base/proxy"))
                }
            }
        }
        Assertions.assertEquals(cookieValues, mapOf("cookie1" to "value1", "cookie2" to "value2"))
    }

    @Test
    fun testResponseCookieDiscardDiffSiteStrict() {
        val requestEntity = HttpEntity(null, null)
        restTemplate.postForEntity(            "http://localhost:$portProxy/base/proxy/cookie?caseId=2",
            requestEntity,
            String::class.java
        ).also { response ->
            Assertions.assertFalse(response.headers.containsKey("Set-Cookie"))
        }
    }

    @Test
    fun testResponseCookieGetSameSiteLax() {
        restTemplate.getForEntity(            "http://localhost:$portProxy/base/proxy/cookie?caseId=3",
            String::class.java
        ).also { response ->
            checkCookieMatch(response)
        }
    }

    @Test
    fun testResponseCookiePostSameSiteLax() {
        val requestEntity = HttpEntity(null, null)
        restTemplate.postForEntity(            "http://localhost:$portProxy/base/proxy/cookie?caseId=3",
            requestEntity,
            String::class.java
        ).also { response ->
            Assertions.assertFalse(response.headers.containsKey("Set-Cookie"))
        }
    }

    @Test
    fun testResponseCookieSameSiteNone() {
        val requestEntity = HttpEntity(null, null)
        restTemplate.postForEntity(            "http://localhost:$portProxy/base/proxy/cookie?caseId=4",
            requestEntity,
            String::class.java
        ).also { response ->
            Assertions.assertFalse(response.headers.containsKey("Set-Cookie"))
        }
    }

    @Test
    fun testResponseCookieRetainSameSiteStrictInsecure() {
        val requestEntity = HttpEntity(null, null)
        restTemplate.postForEntity(            "http://localhost:$portProxy/base/proxy/cookie?caseId=5",
            requestEntity,
            String::class.java
        ).also { response ->
            Assertions.assertFalse(response.headers.containsKey("Set-Cookie"))
        }
    }

}