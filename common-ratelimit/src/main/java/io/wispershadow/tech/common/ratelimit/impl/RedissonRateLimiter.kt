package io.wispershadow.tech.common.ratelimit.impl

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.internal.RateLimiterEventProcessor
import org.redisson.api.RRateLimiter
import org.redisson.api.RateIntervalUnit
import org.redisson.api.RateType
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class RedissonRateLimiter: RateLimiter {
    companion object {
        val logger = LoggerFactory.getLogger(RedissonRateLimiter::class.java)
    }
    private val redissonClient: RedissonClient
    private val name: String
    private val rateLimiterConfig: AtomicReference<RateLimiterConfig>
    private val tags: Map<String, String>
    private val eventProcessor: RateLimiterEventProcessor
    private var rRateLimiter: RRateLimiter

    constructor(name: String,
                redissonClient: RedissonClient,
                config: RateLimiterConfig,
                tags: Map<String, String>) {
        this.name = name
        this.redissonClient = redissonClient
        this.rateLimiterConfig = AtomicReference(config)
        this.tags = tags
        this.eventProcessor = RateLimiterEventProcessor()
        this.rRateLimiter = getRedissonRateLimiter(config)
    }

    constructor(name: String, redissonClient: RedissonClient, rateLimiterConfig: RateLimiterConfig): this(name, redissonClient, rateLimiterConfig, emptyMap<String, String>()) {

    }

    @Synchronized
    override fun changeTimeoutDuration(timeoutDuration: Duration) {
        val updatedConfig = rateLimiterConfig.getAndUpdate{ rateLimiterConfig: RateLimiterConfig ->
            RateLimiterConfig.Builder(
                rateLimiterConfig
            ).timeoutDuration(timeoutDuration).build()
        }
        this.rRateLimiter = getRedissonRateLimiter(updatedConfig)
    }

    @Synchronized
    override fun changeLimitForPeriod(limitForPeriod: Int) {
        val updatedConfig = rateLimiterConfig.getAndUpdate{ rateLimiterConfig: RateLimiterConfig ->
                RateLimiterConfig.Builder(
                    rateLimiterConfig
                ).limitForPeriod(limitForPeriod).build()
        }
        this.rRateLimiter = getRedissonRateLimiter(updatedConfig)
    }


    private fun getRedissonRateLimiter(rateLimiterConfig: RateLimiterConfig): RRateLimiter {
        val limitForPeriod = rateLimiterConfig.limitForPeriod
        val timeWindowDuration = rateLimiterConfig.limitRefreshPeriod
        val rRateLimiter = redissonClient.getRateLimiter(this.name)
        if (!rRateLimiter.isExists) {
            rRateLimiter.trySetRate(
                RateType.OVERALL,
                limitForPeriod.toLong(),
                timeWindowDuration.seconds,
                RateIntervalUnit.SECONDS
            )
            return rRateLimiter
        }
        val existingRateLimiterConfig = rRateLimiter.config
        // 上次配置的限流时间毫秒值
        val rateInterval = existingRateLimiterConfig.rateInterval
        // 上次配置的限流次数
        val rate = existingRateLimiterConfig.rate
        if (rateInterval != timeWindowDuration.toMillis() || rate != limitForPeriod.toLong()) {
            logger.info("Update rate limit config, change limit from {} to {}, change timeout from {} to {}",
                rate, limitForPeriod, rateInterval, timeWindowDuration.toMillis()
            )
            rRateLimiter.delete()
            rRateLimiter.trySetRate(RateType.OVERALL, limitForPeriod.toLong(),
                timeWindowDuration.seconds, RateIntervalUnit.SECONDS
            )
        }
        return rRateLimiter
    }

    override fun acquirePermission(permits: Int): Boolean {
        return rRateLimiter.tryAcquire()
    }

    override fun reservePermission(permits: Int): Long {
        throw UnsupportedOperationException("Reserve permission is not supported by RedissonRateLimiter")
    }

    override fun drainPermissions() {
        rRateLimiter.tryAcquire(rateLimiterConfig.get().limitForPeriod.toLong())
    }

    override fun getName(): String {
        return this.name
    }

    override fun getRateLimiterConfig(): RateLimiterConfig {
        return rateLimiterConfig.get()
    }

    override fun getTags(): Map<String, String> {
        return this.tags
    }

    override fun getMetrics(): RateLimiter.Metrics? {
        return null
    }

    override fun getEventPublisher(): RateLimiter.EventPublisher {
        return this.eventProcessor
    }

}