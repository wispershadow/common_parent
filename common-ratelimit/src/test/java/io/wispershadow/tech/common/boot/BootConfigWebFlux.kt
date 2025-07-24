package io.wispershadow.tech.common.boot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication(scanBasePackages = ["io.wispershadow.tech.common.ratelimit", "io.github.resilience4j.springboot3.ratelimiter.autoconfigure"])
@EnableWebFlux
@EnableAspectJAutoProxy
open class BootConfigWebFlux {
}