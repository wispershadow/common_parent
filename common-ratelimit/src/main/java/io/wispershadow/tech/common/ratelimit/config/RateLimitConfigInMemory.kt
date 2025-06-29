package io.wispershadow.tech.common.ratelimit.config

import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RateLimitConfigInMemory {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RateLimitConfigInMemory::class.java)
    }

    @ConditionalOnMissingClass(value = ["org.redisson.api.RedissonClient"])
    @Bean
    open fun getInMemRateLimiterRegistry(
        rateLimiterProperties: RateLimiterProperties
    ): RateLimiterRegistry {
        logger.info("--------- Start creating in memory rate limiter registry")
        val configs: MutableMap<String, RateLimiterConfig> = HashMap()
        rateLimiterProperties.instances.forEach{(name: String, properties: CommonRateLimiterConfigurationProperties.InstanceProperties) ->
            val rateLimiterConfig =
                RateLimiterConfig.custom()
                    .limitForPeriod(properties.limitForPeriod)
                    .limitRefreshPeriod(properties.limitRefreshPeriod)
                    .timeoutDuration(properties.timeoutDuration)
                    .build()
            configs[name] = rateLimiterConfig
        }
        return RateLimiterRegistry.of(configs)
    }
}