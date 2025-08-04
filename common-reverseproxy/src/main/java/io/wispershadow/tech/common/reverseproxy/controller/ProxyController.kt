package io.wispershadow.tech.common.reverseproxy.controller

import io.wispershadow.tech.common.reverseproxy.ReverseProxyUtils
import io.wispershadow.tech.common.reverseproxy.config.ReverseProxySettingProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.client.RestTemplate
import java.net.URI
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.RestController

@RestController
class ProxyController {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ProxyController::class.java)
    }

    @Autowired
    private lateinit var reverseProxySettingProperties: ReverseProxySettingProperties

    @Autowired
    @Qualifier("proxyDownstreamRestTemplate")
    private lateinit var downstreamRestTemplate: RestTemplate


    @GetMapping("/proxy/**")
    fun proxyGetRequest(request: HttpServletRequest, response: HttpServletResponse) {
        executeRequest(request, response)
    }

    @PostMapping("/proxy/**")
    fun proxyPosRequest(request: HttpServletRequest, response: HttpServletResponse) {
        executeRequest(request, response)
    }

    @PutMapping("/proxy/**")
    fun proxyPutRequest(request: HttpServletRequest, response: HttpServletResponse) {
        executeRequest(request, response)
    }

    @DeleteMapping("/proxy/**")
    fun proxyDeleteRequest(request: HttpServletRequest, response: HttpServletResponse) {
        executeRequest(request, response)
    }


    private fun executeRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val method = request.method
        val proxyRequestUri: String = ReverseProxyUtils.UrlHandler.mapTargetUrlFromRequest(request, reverseProxySettingProperties)
        try {
            val requestEntity: RequestEntity<*> = ReverseProxyUtils.RequestHandler.prepareRequestEntity(
                URI.create(proxyRequestUri), HttpMethod.valueOf(method), request, reverseProxySettingProperties
            )

            val responseEntity: ResponseEntity<Resource> = downstreamRestTemplate.exchange<Resource>(requestEntity,
                Resource::class.java
            )
            ReverseProxyUtils.ResponseHandler.convertResponseEntity(
                responseEntity, request, response, reverseProxySettingProperties
            )
        } catch (e: Exception) {
            logger.error("Error occurred while processing request", e)
            ReverseProxyUtils.ErrorHandler.handleException(
                e, request, response, reverseProxySettingProperties
            )
        }
    }
}