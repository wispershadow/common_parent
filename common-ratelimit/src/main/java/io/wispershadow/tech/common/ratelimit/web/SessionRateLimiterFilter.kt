package io.wispershadow.tech.common.ratelimit.web

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@ConditionalOnClass(name = ["javax.servlet.Filter"])
@ConditionalOnProperty(value = ["wispershadow.ratelimiter.session.enabled"], havingValue = "true", matchIfMissing = false)
class SessionRateLimiterFilter: Filter {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SessionRateLimiterFilter::class.java)
    }

    @Autowired
    private lateinit var rateLimiterRegistry: RateLimiterRegistry


    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse,
                          filterChain: FilterChain
    ) {
        val rateLimiterName = RateLimitKeyGen.session(servletRequest as HttpServletRequest)
        logger.debug("Try acquire rate limiter with name: {}", rateLimiterName)
        return if (rateLimiterRegistry.rateLimiter(rateLimiterName).acquirePermission()) {
            filterChain.doFilter(servletRequest, servletResponse)
        } else {
            logger.warn("Exceed rate limit with name: {}", rateLimiterName)
            (servletResponse as HttpServletResponse).sendError(HttpStatus.FORBIDDEN.value())
        }
    }

    override fun destroy() {
    }
}