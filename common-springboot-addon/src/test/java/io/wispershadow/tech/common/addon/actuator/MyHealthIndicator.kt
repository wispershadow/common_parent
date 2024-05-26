package io.wispershadow.tech.common.addon.actuator

import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status

class MyHealthIndicator: AbstractHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder) {
        val randomValue = Math.random()
        if (randomValue > 0.5) {
            builder.withDetail("pressure", "high").status(Status.OUT_OF_SERVICE)
        }
        else {
            builder.withDetail("pressure", "low").status(Status.UP)
        }
    }
}