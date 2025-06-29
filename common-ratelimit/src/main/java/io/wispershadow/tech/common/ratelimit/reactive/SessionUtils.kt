package io.wispershadow.tech.common.ratelimit.reactive

import org.springframework.http.server.reactive.HttpHandler
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import org.springframework.web.server.adapter.HttpWebHandlerAdapter
import org.springframework.web.server.session.CookieWebSessionIdResolver
import org.springframework.web.server.session.DefaultWebSessionManager
import reactor.core.publisher.Mono
import java.util.*

object SessionUtils {
    private val cookieWebSessionIdResolver: CookieWebSessionIdResolver = CookieWebSessionIdResolver()

    fun getSessionIdFromServerWebExchange(exchange: ServerWebExchange): Optional<String> {
        val sessionIds: List<String> = cookieWebSessionIdResolver.resolveSessionIds(exchange)
        return if (sessionIds.isEmpty()) {
            Optional.empty()
        } else {
            Optional.of(sessionIds[0])
        }
    }

    fun getSessionById(sessionId: String, httpHandler: HttpHandler): Mono<WebSession> {
        return if (httpHandler is HttpWebHandlerAdapter) {
            val sessionManager = httpHandler.sessionManager
            if (sessionManager is DefaultWebSessionManager) {
                sessionManager.sessionStore.retrieveSession(sessionId)
            }
            else {
                Mono.empty()
            }
        }
        else {
            Mono.empty()
        }
    }
}