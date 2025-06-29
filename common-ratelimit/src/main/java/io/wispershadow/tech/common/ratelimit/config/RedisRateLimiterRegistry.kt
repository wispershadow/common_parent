package io.wispershadow.tech.common.ratelimit.config

import io.github.resilience4j.core.RegistryStore
import io.github.resilience4j.core.registry.RegistryEventConsumer
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.internal.InMemoryRateLimiterRegistry
import io.wispershadow.tech.common.ratelimit.impl.RedissonRateLimiter
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import java.util.function.Supplier

class RedisRateLimiterRegistry(val redissonClient: RedissonClient,
                               configs: Map<String, RateLimiterConfig>,
                               registryEventConsumers: List<RegistryEventConsumer<RateLimiter>>,
                               tags: Map<String, String>,
                               registryStore: RegistryStore<RateLimiter>
): InMemoryRateLimiterRegistry(configs, registryEventConsumers, tags, registryStore) {
    companion object {
        val logger = LoggerFactory.getLogger(RedisRateLimiterRegistry::class.java)
    }

    override fun rateLimiter(name: String): RateLimiter  {
        val index = name.lastIndexOf("_")
        var keyPart = name
        var configSuffix = name
        if (index >= 0) {
            keyPart = name.substring(0, index)
            configSuffix = name.substring(index + 1)
        }
        val configOptional = this.getConfiguration(configSuffix)
        val actualConfig = configOptional.orElseGet(Supplier { this.defaultConfig })
        if (logger.isDebugEnabled) {
            logger.debug(
                "Creating rate limiter instance with name: {}, limit: {}, timeWindow: {}, timeOut: {}", name,
                actualConfig.limitForPeriod, actualConfig.limitRefreshPeriod, actualConfig.timeoutDuration
            )
        }
        return this.rateLimiter(keyPart, actualConfig)

    }

    override fun rateLimiter(name: String, config: RateLimiterConfig, tags: MutableMap<String, String>): RateLimiter {
        return this.computeIfAbsent(name) {
            RedissonRateLimiter(name, redissonClient, config, this.getAllTags(tags))
        }
    }

    override fun rateLimiter(name: String, rateLimiterConfigSupplier: Supplier<RateLimiterConfig>, tags: Map<String, String>): RateLimiter {
        return this.computeIfAbsent(name) {
            RedissonRateLimiter(name, redissonClient, rateLimiterConfigSupplier.get(), this.getAllTags(tags))
        }
    }
}