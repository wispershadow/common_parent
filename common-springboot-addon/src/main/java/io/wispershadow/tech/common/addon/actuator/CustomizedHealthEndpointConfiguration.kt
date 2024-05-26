package io.wispershadow.tech.common.addon.actuator

import org.springframework.boot.actuate.health.HealthContributorRegistry
import org.springframework.boot.actuate.health.HealthEndpointGroups
import org.springframework.boot.actuate.health.HealthEndpointWebExtension
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class CustomizedHealthEndpointConfiguration {

    @Bean
    @ConditionalOnMissingBean
    open fun healthEndpointWebExtension(
        healthContributorRegistry: HealthContributorRegistry,
        groups: HealthEndpointGroups
    ): HealthEndpointWebExtension {
        return LoggingHealthEndpointWebExtension(healthContributorRegistry, groups)
    }
}