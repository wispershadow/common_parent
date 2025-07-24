package io.wispershadow.tech.common.testbackend.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication(scanBasePackages = ["io.wispershadow.tech.common.testbackend"])
@EnableWebFlux
open class BootConfigWebFlux {

}