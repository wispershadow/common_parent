package io.wispershadow.tech.common.ratelimit.config

import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties
import io.github.resilience4j.core.registry.InMemoryRegistryStore
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.spring6.spelresolver.SpelResolver
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties
import io.wispershadow.tech.common.ratelimit.impl.SimpleSpelResolver
import org.redisson.api.RedissonClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.ParameterNameDiscoverer
import org.springframework.core.StandardReflectionParameterNameDiscoverer
import org.springframework.expression.spel.standard.SpelExpressionParser

@Configuration
open class RateLimitConfigRedis {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RateLimitConfigRedis::class.java)
    }

    @Bean
    @Primary
    @ConditionalOnClass(RedissonClient::class)
    open fun getRedisRateLimiterRegistry(
        redissonClient: RedissonClient,
        rateLimiterProperties: RateLimiterProperties
    ): RedisRateLimiterRegistry {
        logger.info("--------- Start creating redis rate limiter registry")
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
        val defaultStore = InMemoryRegistryStore<RateLimiter>()
        return RedisRateLimiterRegistry(redissonClient, configs, emptyList(), emptyMap(), defaultStore)
    }



    @Bean
    @Primary
    @ConditionalOnClass(RedissonClient::class)
    open fun spelResolver(
        spelExpressionParser: SpelExpressionParser,
        parameterNameDiscoverer: ParameterNameDiscoverer
    ): SpelResolver {
        return SimpleSpelResolver(spelExpressionParser, parameterNameDiscoverer)
    }

    @Bean
    @Primary
    @ConditionalOnClass(RedissonClient::class)
    open fun spelExpressionParser(): SpelExpressionParser {
        return SpelExpressionParser()
    }

    @Bean
    @Primary
    @ConditionalOnClass(RedissonClient::class)
    open fun parameterNameDiscoverer(): ParameterNameDiscoverer {
        return StandardReflectionParameterNameDiscoverer()
    }
}