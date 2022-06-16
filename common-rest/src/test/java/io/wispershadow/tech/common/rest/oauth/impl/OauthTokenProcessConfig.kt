package io.wispershadow.tech.common.rest.oauth.impl

import io.wispershadow.tech.common.rest.oauth.OauthToken
import io.wispershadow.tech.common.rest.oauth.OauthTokenAccqConfigData
import io.wispershadow.tech.common.rest.oauth.OauthTokenManager
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@EnableConfigurationProperties
open class OauthTokenProcessConfig {

    @Bean
    @ConfigurationProperties(prefix = "integration.oauth")
    open fun oauthTokenAccqConfigData(): OauthTokenAccqConfigData {
        return OauthTokenAccqConfigData()
    }

    @Bean
    open fun oauthTokenManager(oauthTokenAccqConfigData: OauthTokenAccqConfigData): OauthTokenManager {
        return object: AbstractOauthTokenManager(oauthTokenAccqConfigData) {
            override fun acquireOauthToken(clientId: String): OauthToken {
                val token = if (clientId == "12345") {
                    "abcde"
                }
                else {
                    UUID.randomUUID().toString()
                }
                return DefaultOauthToken(System.currentTimeMillis(), 30000L, token)
            }
        }
    }

    @Bean
    open fun registerTokenPostProcessor(oauthTokenAccqConfigData: OauthTokenAccqConfigData, oauthTokenManager: OauthTokenManager): RegisterTokenAcquisitionForRestTemplatePostProcessor {
        return RegisterTokenAcquisitionForRestTemplatePostProcessor(oauthTokenAccqConfigData, oauthTokenManager)
    }
}