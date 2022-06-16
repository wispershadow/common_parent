package io.wispershadow.tech.common.rest.oauth.impl

import io.wispershadow.tech.common.rest.oauth.OauthTokenAccqConfigData
import io.wispershadow.tech.common.rest.oauth.OauthTokenManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate

open class RegisterTokenAcquisitionForRestTemplatePostProcessor(val oauthTokenAccqConfigData: OauthTokenAccqConfigData,
                                                                val oauthTokenManager: OauthTokenManager
): BeanPostProcessor {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RegisterTokenAcquisitionForRestTemplatePostProcessor::class.java)
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        if (bean is RestTemplate) {
            val clientId = matchClientId(beanName, oauthTokenAccqConfigData)
            if (clientId != null) {
                val oauthBearerTokenInterceptor = OauthBearerTokenInterceptor(oauthTokenAccqConfigData.oauthTokenHeaderName, oauthTokenManager, { request, body -> clientId})
                val restTemplateInterceptors = if (bean.interceptors == null) {
                    mutableListOf<ClientHttpRequestInterceptor>()
                }
                else {
                    bean.interceptors
                }
                logger.info("Register oauthBearerTokenInterceptor for bean: {}", beanName)
                restTemplateInterceptors.add(oauthBearerTokenInterceptor)
                bean.interceptors = restTemplateInterceptors
            }
            else {
                logger.warn("No matching client id found, no interceptor registered for bean: {}", beanName)
            }
        }
        return bean
    }

    open fun matchClientId(beanName: String, oauthTokenAccqConfigData: OauthTokenAccqConfigData): String? {
        val matchingEntryExact = oauthTokenAccqConfigData.clientIdMappings.firstOrNull { clientIdMapping ->
            clientIdMapping.applicableBeanNames.contains(beanName)
        }
        if (matchingEntryExact != null) {
            return matchingEntryExact.clientId
        }
        val matchingEntryFuzzy = oauthTokenAccqConfigData.clientIdMappings.firstOrNull { clientIdMappingData ->
            clientIdMappingData.applicableBeanNames.size == 1 &&
            clientIdMappingData.applicableBeanNames.contains("*")
        }
        if (matchingEntryFuzzy != null) {
            return matchingEntryFuzzy.clientId
        }
        return null
    }
}