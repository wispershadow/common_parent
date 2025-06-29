package io.wispershadow.tech.common.ratelimit.config

import io.wispershadow.tech.common.utils.SpringContextUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RateLimitCommonConfig {
    @Bean
    open fun getSpringContextUtils(): SpringContextUtils {
        return SpringContextUtils
    }
}