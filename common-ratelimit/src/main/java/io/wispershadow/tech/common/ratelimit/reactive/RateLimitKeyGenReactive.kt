package io.wispershadow.tech.common.ratelimit.reactive

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

object RateLimitKeyGenReactive {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitKeyGenReactive::class.java)

    private const val SUFFIX_IP = "ip"
    private const val SUFFIX_SESSION = "session"
    private const val PREFIX_PATH = "path"
    private const val DEFAULT_NAME = "default";
    private const val PROXY_HEADER_NAME = "X-FORWARDED-FOR"

    @JvmStatic
    fun ip(parameter: Any): String {
        when (parameter) {
            is ServerWebExchange -> {
                val proxyIpHeaderValue = parameter.request.headers[PROXY_HEADER_NAME]
                if (!proxyIpHeaderValue.isNullOrEmpty()) {
                    logger.debug("Getting ip from proxy header")
                    return "${proxyIpHeaderValue[0]}_${SUFFIX_IP}"
                }

                val ipAddrValue = parameter.request.remoteAddress?.address?.hostAddress
                return Optional.ofNullable(ipAddrValue).map {
                    "${it}_${SUFFIX_IP}"
                }.orElseGet { DEFAULT_NAME }
            }
            is ServerHttpRequest -> {
                val proxyIpHeaderValue = parameter.headers[PROXY_HEADER_NAME]
                if (!proxyIpHeaderValue.isNullOrEmpty()) {
                    return "${proxyIpHeaderValue[0]}_${SUFFIX_IP}"
                }

                val ipAddrValue = parameter.remoteAddress?.address?.hostAddress
                return Optional.ofNullable(ipAddrValue).map {
                    "${it}_${SUFFIX_IP}"
                }.orElseGet { DEFAULT_NAME }
            }
            else -> {
                logger.error("Unknown parameter type: {} for ip, expected ServetWebExchange or ServerHttpRequest",
                    parameter.javaClass
                )
                return DEFAULT_NAME
            }
        }
    }

    @JvmStatic
    fun session(parameter: Any): String {
        when (parameter) {
            is ServerWebExchange -> {
                val sessionIdOptional: Optional<String> = SessionUtils.getSessionIdFromServerWebExchange(parameter)
                val resultQueue = ArrayBlockingQueue<String>(1)
                return sessionIdOptional.map { sessionId ->
                    SessionUtils.checkSessionExists(sessionId).subscribe { exists ->
                        if (exists) {
                            resultQueue.offer("${sessionId}_${SUFFIX_SESSION}")
                        }
                    }
                    resultQueue.poll(200, TimeUnit.MILLISECONDS)
                        ?: throw RuntimeException("No web session found for sessionId")
                }.orElseGet {
                    logger.error("No WebSession Cookie found in ServerWebExchange, using default session key")
                    DEFAULT_NAME
                }
            }
            is WebSession -> {
                return  "${parameter.id}_${SUFFIX_SESSION}"
            }
            else -> {
                logger.error("Unknown parameter type: {} for session, expected ServetWebExchange or ServerHttpRequest",
                    parameter.javaClass
                )
                return DEFAULT_NAME
            }
        }
    }

    @JvmStatic
    fun path(parameter: Any): String {
        return if (parameter is ServerHttpRequest) {
            "${PREFIX_PATH}_${parameter.path.value()}"
        } else {
            logger.error("Unknown parameter type: {} for path, expected ServerHttpRequest",
                parameter.javaClass
            )
            DEFAULT_NAME
        }
    }

}