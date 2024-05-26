package io.wispershadow.tech.common.addon.actuator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.endpoint.http.ApiVersion
import org.springframework.boot.actuate.health.*

class LoggingHealthEndpointWebExtension(registry: HealthContributorRegistry, groups: HealthEndpointGroups): HealthEndpointWebExtension(registry, groups) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LoggingHealthEndpointWebExtension::class.java)
    }

    override fun aggregateContributions(
        apiVersion: ApiVersion, contributions: Map<String, HealthComponent>,
        statusAggregator: StatusAggregator, showComponents: Boolean, groupNames: Set<String>?
    ): HealthComponent {
        val compositeHealth: CompositeHealth = getCompositeHealth(apiVersion, contributions, statusAggregator, showComponents, groupNames)
        if (compositeHealth.status != Status.UP) {
            contributions.forEach { (componentKey, healthComponent) ->
                if (healthComponent.status != Status.UP) {
                    if (healthComponent is Health) {
                        logger.info("Health component {} status is: {},  detail is: {}", componentKey, healthComponent.status, healthComponent.details)
                    }
                    else {
                        logger.info("Health component {} status is: {}", componentKey, healthComponent.status)
                    }
                }
            }
        }
        return compositeHealth
    }
}