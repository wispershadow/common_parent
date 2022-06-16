package io.wispershadow.tech.common.rest.oauth.impl

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
open class TestRestTemplateConfig {
    @Bean("testRestTemplate1")
    open fun restTemplate1(): RestTemplate {
        return RestTemplate()
    }

    @Bean("testRestTemplate2")
    open fun restTemplate2(): RestTemplate {
        return RestTemplate()
    }
}