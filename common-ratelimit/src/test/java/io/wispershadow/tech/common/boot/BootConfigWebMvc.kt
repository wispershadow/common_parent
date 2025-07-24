package io.wispershadow.tech.common.boot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication(scanBasePackages = ["io.wispershadow.tech.common.ratelimit", "io.github.resilience4j.springboot3.ratelimiter.autoconfigure"])
@EnableWebMvc
@EnableAspectJAutoProxy
open class BootConfigWebMvc {
}