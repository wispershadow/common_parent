package io.wispershadow.tech.common.ratelimit.reactive

import io.wispershadow.tech.common.utils.SpringContextUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import org.springframework.web.server.adapter.HttpWebHandlerAdapter
import org.springframework.web.server.session.CookieWebSessionIdResolver
import org.springframework.web.server.session.DefaultWebSessionManager
import org.springframework.web.server.session.WebSessionManager
import org.springframework.web.server.session.WebSessionStore
import reactor.core.publisher.Mono
import java.util.*

object SessionUtils {
    private val logger: Logger = LoggerFactory.getLogger(SessionUtils::class.java)

    private val cookieWebSessionIdResolver: CookieWebSessionIdResolver = CookieWebSessionIdResolver()

    fun getSessionIdFromServerWebExchange(exchange: ServerWebExchange): Optional<String> {
        val sessionIds: List<String> = cookieWebSessionIdResolver.resolveSessionIds(exchange)
        return if (sessionIds.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(sessionIds[0])
        }
    }


    fun checkSessionExists(sessionId: String): Mono<Boolean> {
        val webSessionManagerBean = SpringContextUtils.getBean(WebSessionManager::class.java)
        if (webSessionManagerBean is DefaultWebSessionManager) {
            val webSessionStore = webSessionManagerBean.sessionStore
            return checkSessionExists(sessionId, webSessionStore)
        }
        val httpHandler = SpringContextUtils.getBean(HttpHandler::class.java)
        if (httpHandler is HttpWebHandlerAdapter) {
            val webSessionManager = httpHandler.sessionManager
            if (webSessionManager is DefaultWebSessionManager) {
                val webSessionStore = webSessionManager.sessionStore
                return checkSessionExists(sessionId, webSessionStore)
            }
        }

        return Mono.just(false)

    }

    private fun checkSessionExists(sessionId: String, webSessionStore: WebSessionStore): Mono<Boolean> {
        return webSessionStore.retrieveSession(sessionId)
            .map { session -> true }
            .onErrorReturn(false)
            .defaultIfEmpty(false)
    }
}