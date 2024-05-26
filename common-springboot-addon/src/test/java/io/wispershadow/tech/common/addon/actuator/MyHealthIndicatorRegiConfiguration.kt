package io.wispershadow.tech.common.addon.actuator

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnEnabledHealthIndicator("my")
@Configuration
class MyHealthIndicatorRegiConfiguration {
    @Bean
    open fun myHealthIndicator(): MyHealthIndicator {
        return MyHealthIndicator()
    }
}