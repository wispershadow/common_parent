package io.wispershadow.tech.common.ratelimit.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource

@TestConfiguration
open class RedisClientConfig {
    @Bean(value = ["httpSessionRedissonClient"], destroyMethod = "shutdown")
    open fun redisson(@Value("classpath:/redisson.yml") configFile: Resource): RedissonClient {
        val config = Config.fromYAML(configFile.inputStream)
        return Redisson.create(config)
    }
}