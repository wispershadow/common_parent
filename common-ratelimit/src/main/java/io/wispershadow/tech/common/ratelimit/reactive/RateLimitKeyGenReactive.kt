package io.wispershadow.tech.common.ratelimit.reactive

import io.wispershadow.tech.common.utils.SpringContextUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

object RateLimitKeyGenReactive {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitKeyGenReactive::class.java)

    private const val SUFFIX_IP = "ip"
    private const val SUFFIX_SESSION = "session"
    private const val SUFFIX_PATH = "path"
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
                val httpHandler = SpringContextUtils.getBean(HttpHandler::class.java)
                val sessionIdOptional: Optional<String> = SessionUtils.getSessionIdFromServerWebExchange(parameter)
                val dataQueue = ArrayBlockingQueue<String>(1)
                return sessionIdOptional.map { sessionId ->
                    SessionUtils.getSessionById(sessionId, httpHandler).map {
                        "${sessionId}_${SUFFIX_SESSION}"
                    }.switchIfEmpty (
                        Mono.error(RuntimeException("No web session found for sessionId"))
                    ).subscribe {
                        dataQueue.add(it)
                    }
                    dataQueue.poll(100, TimeUnit.MILLISECONDS)

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
            "${parameter.path.value()}_${SUFFIX_PATH}"
        } else {
            logger.error("Unknown parameter type: {} for path, expected ServerHttpRequest",
                parameter.javaClass
            )
            DEFAULT_NAME
        }
    }

}