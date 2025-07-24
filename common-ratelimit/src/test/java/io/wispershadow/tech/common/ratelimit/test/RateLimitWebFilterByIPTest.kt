package io.wispershadow.tech.common.ratelimit.test

import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties
import io.wispershadow.tech.common.boot.BootConfigWebFlux
import io.wispershadow.tech.common.ratelimit.config.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

@EnableAutoConfiguration
@EnableConfigurationProperties(RateLimiterProperties::class)
@SpringBootTest(classes = [BootConfigWebFlux::class,
    RedisClientConfig::class
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.main.web-application-type=reactive",
    "spring.main.allow-bean-definition-overriding=true"
])
class RateLimitWebFilterByIPTest {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitWebFilterByIPTest::class.java)

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
        lateinit var redisServerConfig: RedisServerConfig

        @JvmStatic
        @BeforeAll
        fun beforeTest() {
            System.setProperty("wispershadow.ratelimiter.ip.enabled", "true")
            redisServerConfig = RedisServerConfig().apply {
                this.postConstruct()
            }
        }

        @JvmStatic
        @AfterAll
        fun afterTest() {
            System.setProperty("wispershadow.ratelimiter.ip.enabled", "false")
            redisServerConfig.preDestroy()
        }
    }

    @Test
    fun testRateLimitByIP() {
        val sleepInterval = arrayOf(0L, 100L, 100L, 1000L, 2000L)
        val expectedResult = arrayOf(true, true, true, false, true)
        val actualResult = arrayOf(false, false, false, false, false)
        for (i in sleepInterval.indices) {
            Thread.sleep(sleepInterval[i])
            webTestClient.get()
                .uri("/byip")
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

}