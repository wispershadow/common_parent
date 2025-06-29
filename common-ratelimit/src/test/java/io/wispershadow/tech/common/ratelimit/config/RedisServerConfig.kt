package io.wispershadow.tech.common.ratelimit.config

import org.springframework.boot.test.context.TestConfiguration
import redis.embedded.RedisServer
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@TestConfiguration
open class RedisServerConfig {
    private val redisServer: RedisServer = RedisServer(9999)

    @PostConstruct
    open fun postConstruct() {
        redisServer.start()
    }

    @PreDestroy
    open fun preDestroy() {
        redisServer.stop()
    }

}