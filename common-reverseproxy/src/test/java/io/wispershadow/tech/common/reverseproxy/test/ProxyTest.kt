package io.wispershadow.tech.common.reverseproxy.test

import io.wispershadow.tech.common.reverseproxy.config.BootConfigWebMvc
import io.wispershadow.tech.common.testbackend.config.BootConfigWebFlux
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

class ProxyTest {
    private val restTemplate = RestTemplate()

    companion object {
        private lateinit var applContextProxy: ConfigurableApplicationContext

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
            println(fileContent)
        } ?: run {
            Assertions.fail("Response body is null")
        }
    }

    @Test
    fun testDownLoadFileNotFound() {

    }

    @Test
    fun testProxyRequestTimeout() {

    }


    fun testResponseCookie() {

    }

    fun testRequestHeaderRemove() {

    }

}