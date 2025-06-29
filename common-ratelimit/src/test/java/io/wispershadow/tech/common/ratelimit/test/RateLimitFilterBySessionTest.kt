package io.wispershadow.tech.common.ratelimit.test

import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties
import io.wispershadow.tech.common.ratelimit.BootConfig
import io.wispershadow.tech.common.ratelimit.config.RedisClientConfig
import io.wispershadow.tech.common.ratelimit.config.RedisServerConfig
import org.junit.jupiter.api.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import java.time.Duration

@EnableAutoConfiguration
@EnableConfigurationProperties(RateLimiterProperties::class)
@SpringBootTest(classes = [BootConfig::class, RedisServerConfig::class,
    RedisClientConfig::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitFilterBySessionTest {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitFilterBySessionTest::class.java)

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @LocalServerPort
    private var port: Int = -1

    @BeforeEach
    fun setWebTestClientTimeout() {
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofMillis(30000))
            .build()
    }


    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeTest() {
            System.setProperty("wispershadow.ratelimiter.session.enabled", "true")
        }

        @JvmStatic
        @AfterAll
        fun afterTest() {
            System.setProperty("wispershadow.ratelimiter.session.enabled", "false")
        }
    }


    @Test
    fun testRateLimitByFakeSessionId() {
        val response = sendRequestWithSessionCookie("12345")
        Assertions.assertEquals (500, response.rawStatusCode)
    }

    @Test
    fun testRateLimitBySession() {
        val sleepInterval = arrayOf(0L, 100L, 1000L, 1000L)
        val expectedResult = arrayOf(true, true, false, true)
        val actualResult = arrayOf(false, false, false, false)
        webTestClient.get()
            .uri("/createsession")
            .exchange()
            .expectBody()
            .consumeWith {response ->
                val sessionCookies = response.responseCookies["SESSION"]!!
                val sessionId = sessionCookies[0].value
                for (i in sleepInterval.indices) {
                    Thread.sleep(sleepInterval[i])
                    val response = sendRequestWithSessionCookie(sessionId)
                    if (200 == response.rawStatusCode) {
                        actualResult[i] = true
                        response.responseBody?.let {
                            logger.info("Received response: {}",  String(it, Charsets.UTF_8))
                        }
                    }
                    else {
                        logger.info("Response code received: {}", response.rawStatusCode)
                    }
                }
            }
        Assertions.assertArrayEquals(expectedResult, actualResult)
    }

    private fun sendRequestWithSessionCookie(sessionId: String): EntityExchangeResult<ByteArray> {
        return webTestClient.get()
            .uri("/byip")
            .cookies {cookies ->
                cookies.add("SESSION", sessionId)
            }
            .exchange()
            .expectBody()
            .returnResult()
    }
}