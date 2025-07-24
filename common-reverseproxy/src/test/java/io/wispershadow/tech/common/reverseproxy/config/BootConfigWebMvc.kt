package io.wispershadow.tech.common.reverseproxy.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication(scanBasePackages = ["io.wispershadow.tech.common.reverseproxy"])
@EnableWebMvc
open class BootConfigWebMvc {

}