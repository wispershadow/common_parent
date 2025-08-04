package io.wispershadow.tech.common.ratelimit.reactive

import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@ConditionalOnProperty(value = ["wispershadow.ratelimiter.session.enabled"], havingValue = "true", matchIfMissing = false)
class SessionIdRateLimiterWebFilter: WebFilter {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SessionIdRateLimiterWebFilter::class.java)
    }

    @Autowired
    private lateinit var rateLimiterRegistry: RateLimiterRegistry


    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return SessionUtils.getSessionIdFromServerWebExchange(exchange)
            .map { sessionId ->
               SessionUtils.checkSessionExists(sessionId).flatMap {exists ->
                   if (exists) {
                       doRateLimit(exchange, chain)
                   }
                   else {
                       throw RuntimeException("No web session found for sessionId")
                   }
               }
            }.orElseGet {
                logger.debug("No session id found in request, fallback to use default config")
                doRateLimit(exchange, chain)
            }
    }

    fun doRateLimit(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val rateLimiterName = RateLimitKeyGenReactive.session(exchange)
        logger.debug("Try acquire rate limiter with name: {}", rateLimiterName)
        return if (rateLimiterRegistry.rateLimiter(rateLimiterName).acquirePermission()) {
            chain.filter(exchange)
        } else {
            logger.warn("Exceed rate limit with name: {}", rateLimiterName)
            Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "Rate limit reached"))
        }
    }
}