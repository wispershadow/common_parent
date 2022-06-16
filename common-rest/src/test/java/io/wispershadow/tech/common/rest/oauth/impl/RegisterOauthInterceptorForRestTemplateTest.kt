package io.wispershadow.tech.common.rest.oauth.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate

@ExtendWith(
    SpringExtension::class)
@SpringBootTest(classes = [OauthTokenProcessConfig::class, TestRestTemplateConfig::class])
@ActiveProfiles("test")
class RegisterOauthInterceptorForRestTemplateTest {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegisterOauthInterceptorForRestTemplateTest::class.java)
    }

    @Autowired
    @Qualifier("testRestTemplate1")
    lateinit var restTemplate1: RestTemplate

    @Autowired
    @Qualifier("testRestTemplate2")
    lateinit var restTemplate2: RestTemplate

    @Test
    fun testInjectTokenInterceptor() {
        val matchedInterceptor1 =
            restTemplate1.interceptors.find { it::class.java == OauthBearerTokenInterceptor::class.java }
        val matchedInterceptor2 =
            restTemplate2.interceptors.find { it::class.java == OauthBearerTokenInterceptor::class.java }
        Assertions.assertNotNull(matchedInterceptor1)
        Assertions.assertNull(matchedInterceptor2)
        val headers = mutableMapOf<String, Any>()
        val inspectHeaderInterceptor =
            ClientHttpRequestInterceptor { request, body, execution ->
                request.headers.forEach { name, value ->
                    headers[name] = value
                }
                execution.execute(request, body)
            }
        restTemplate1.interceptors.add(inspectHeaderInterceptor)
        try {
            //send dummy request
            restTemplate1.getForEntity("http://127.0.0.1:8080", String::class.java)
        }
        catch (e: Exception) {
        }
        logger.info("Getting http headers: {}", headers)
        Assertions.assertEquals(headers["Authorization"], listOf("Bearer abcde"))
    }
}