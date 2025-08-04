package io.wispershadow.tech.common.ratelimit.test

import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties
import io.wispershadow.tech.common.boot.BootConfigWebFlux
import io.wispershadow.tech.common.ratelimit.config.RedisClientConfig
import io.wispershadow.tech.common.ratelimit.config.RedisServerConfig
import org.junit.jupiter.api.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

@EnableAutoConfiguration
@EnableConfigurationProperties(RateLimiterProperties::class)
@SpringBootTest(classes = [BootConfigWebFlux::class,
    RedisClientConfig::class
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.main.web-application-type=reactive",
    "spring.main.allow-bean-definition-overriding=true"
])
class RateLimitByAnnotationTest {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitByAnnotationTest::class.java)

    @Autowired
    private lateinit var webTestClient: WebTestClient

    companion object {

        @JvmStatic
        lateinit var redisServerConfig: RedisServerConfig

        @JvmStatic
        @BeforeAll
        fun beforeTest() {
            redisServerConfig = RedisServerConfig().apply {
                this.postConstruct()
            }
        }

        @JvmStatic
        @AfterAll
        fun afterTest() {
            redisServerConfig.preDestroy()
        }
    }

    @BeforeEach
    fun setWebTestClientTimeout() {
        webTestClient = webTestClient.mutate()
            .responseTimeout(Duration.ofMillis(30000))
            .build()
    }


    @Test
    fun testRateLimitByIp() {
        val sleepInterval = arrayOf(0L, 100L, 100L, 1000L, 2000L)
        val expectedResult = arrayOf(true, true, true, false, true)
        val actualResult = arrayOf(false, false, false, false, false)
        for (i in sleepInterval.indices) {
            Thread.sleep(sleepInterval[i])
            webTestClient.get()
                .uri("/byip_anno")
                .exchange()
                .expectBody()
                .consumeWith { response ->
                    if (200 == response.status.value()) {
                        actualResult[i] = true
                        response.responseBody?.let {
                            logger.info("Received response: {}",  String(it, Charsets.UTF_8))
                        }
                    }
                    else {
                        logger.info("Response code received: {}", response.status.value())
                    }
                }
        }
        Assertions.assertArrayEquals(expectedResult, actualResult)

    }


    @Test
    fun testRateLimitFakeSession() {
       val response = sendRequestWithSessionCookie("12345")
        Assertions.assertEquals(response.status.value(), 403)
        response.responseBody?.let {
            logger.info("Received response: {}", String(it, Charsets.UTF_8))
        }
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
            .consumeWith { response ->
                val sessionCookies = response.responseCookies["SESSION"]!!
                val sessionId = sessionCookies[0].value
                for (i in sleepInterval.indices) {
                    Thread.sleep(sleepInterval[i])
                    val response = sendRequestWithSessionCookie(sessionId)
                    if (200 == response.status.value()) {
                        actualResult[i] = true
                        response.responseBody?.let {
                            logger.info("Received response: {}",  String(it, Charsets.UTF_8))
                        }
                    }
                    else {
                        logger.info("Response code received: {}", response.status.value())
                    }
                }
            }
        Assertions.assertArrayEquals(expectedResult, actualResult)
    }


    @Test
    fun testRateLimitByPath() {
        val sleepInterval = arrayOf(0L, 100L, 4000L)
        val expectedResult = arrayOf(true, false, true)
        val actualResult = arrayOf(false, false, false)
        for (i in sleepInterval.indices) {
            Thread.sleep(sleepInterval[i])
            webTestClient.get()
                .uri("/api/path1")
                .exchange()
                .expectBody()
                .consumeWith { response ->
                    if (200 == response.status.value()) {
                        actualResult[i] = true
                        response.responseBody?.let {
                            logger.info("Received response: {}",  String(it, Charsets.UTF_8))
                        }
                    }
                    else {
                        logger.info("Response code received: {}", response.status.value())
                    }
                }
        }
        Assertions.assertArrayEquals(expectedResult, actualResult)

    }


    private fun sendRequestWithSessionCookie(sessionId: String): EntityExchangeResult<ByteArray> {
        return webTestClient.get()
            .uri("/bysession_anno")
            .cookies {cookies ->
                cookies.add("SESSION", sessionId)
            }
            .exchange()
            .expectBody()
            .returnResult()
    }
}