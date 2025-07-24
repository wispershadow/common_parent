package io.wispershadow.tech.common.ratelimit.config

import io.wispershadow.tech.common.ratelimit.controller.TestWebController


//@TestConfiguration
open class ReactiveRestControllerTestConfig {

    //@Bean
    open fun testController(): TestWebController {
        return TestWebController();
    }

}