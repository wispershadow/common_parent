package io.wispershadow.tech.common.reverseproxy.config

import io.wispershadow.tech.common.reverseproxy.ApacheClientBuilder
import org.apache.hc.client5.http.classic.HttpClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(
    ApacheClientSettingProperties::class,
    ReverseProxySettingProperties::class
)
open class ProxyRestTemplateConfig {

    @Bean("proxyDownstreamRestTemplate")
    open fun getDownstreamRestTemplate(
        apacheClientSettingProperties: ApacheClientSettingProperties,
        reverseProxySettingProperties: ReverseProxySettingProperties
    ): RestTemplate {
        val httpClient: HttpClient =
            ApacheClientBuilder.createHttpClient(apacheClientSettingProperties, reverseProxySettingProperties)
        val factory = HttpComponentsClientHttpRequestFactory(httpClient)
        return RestTemplate(factory)
    }

}