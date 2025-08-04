package io.wispershadow.tech.common.ratelimit.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.servlet.http.HttpServletRequest

object RateLimitKeyGen {
    private val logger: Logger = LoggerFactory.getLogger(RateLimitKeyGen::class.java)

    private const val SUFFIX_IP = "ip"
    private const val SUFFIX_SESSION = "session"
    private const val PREFIX_PATH = "path"
    private const val DEFAULT_NAME = "default";
    private const val PROXY_HEADER_NAME = "X-FORWARDED-FOR"

    @JvmStatic
    fun ip(parameter: HttpServletRequest): String {
        val proxyIpHeader = parameter.getHeader(PROXY_HEADER_NAME)
        if (proxyIpHeader!= null) {
            logger.debug("Getting ip from proxy header")
            return "${proxyIpHeader}__${SUFFIX_IP}"
        }
        return Optional.ofNullable(parameter.remoteAddr).map {
            "${it}_${SUFFIX_IP}"
        }.orElseGet {
            DEFAULT_NAME
        }

    }

    @JvmStatic
    fun session(parameter: HttpServletRequest): String {
        val session = parameter.getSession(false)
        if (session != null) {
            return "${session.id}__${SUFFIX_SESSION}"
        }
        else {
            throw RuntimeException("No web session found")
        }
    }

    @JvmStatic
    fun path(parameter: HttpServletRequest): String {
        return Optional.ofNullable(parameter.pathInfo).map {
            "${PREFIX_PATH}_${it}"
        }.orElseGet {
            DEFAULT_NAME
        }
    }
}