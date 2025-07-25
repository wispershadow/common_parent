package io.wispershadow.tech.common.ratelimit.test

import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties
import io.wispershadow.tech.common.boot.BootConfigWebMvc
import io.wispershadow.tech.common.ratelimit.config.RedisClientConfig
import io.wispershadow.tech.common.ratelimit.config.RedisServerConfig
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.EnabledIf
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration


@EnableConfigurationProperties(RateLimiterProperties::class)
@SpringBootTest(classes = [BootConfigWebMvc::class,
    RedisClientConfig::class
], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@EnabledIf(expression = "#{systemProperties['environment'] == 'nonreactive'}")
@TestPropertySource(properties = [
    "spring.main.web-application-type=servlet",
    "spring.main.allow-bean-definition-overriding=true"
])
class RateLimitFilterByIPTest {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitFilterByIPTest::class.java)

    @Autowired
    private lateinit var webTestClient: WebTestClient


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
    fun test() {
        webTestClient.get()
            .uri("/byip_standard")
            .exchange()
            .expectBody()
            .consumeWith { response ->
                println(response.status.value())
            }

    }
}