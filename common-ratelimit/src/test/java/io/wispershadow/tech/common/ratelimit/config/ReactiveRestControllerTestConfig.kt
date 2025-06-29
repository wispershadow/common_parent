package io.wispershadow.tech.common.ratelimit.config

import io.wispershadow.tech.common.ratelimit.controller.TestController
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean


@TestConfiguration
open class ReactiveRestControllerTestConfig {

    @Bean
    open fun testController(): TestController {
        return TestController();
    }

}