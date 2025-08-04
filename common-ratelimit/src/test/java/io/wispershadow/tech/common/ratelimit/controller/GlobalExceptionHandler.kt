package io.wispershadow.tech.common.ratelimit.controller

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Mono

//used to handle exception thrown early in aspect before AOP joinPoint.proceed executes
//note that normal global exception handler won't cNo web session found for sessionIdatch this exception
@Component
@Order(-1)
class GlobalExceptionHandler: ErrorWebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        if (ex is RuntimeException) {
            if ("No web session found for sessionId" == ex.message) {
                val response = exchange.response
                response.apply {
                    this.statusCode = org.springframework.http.HttpStatus.FORBIDDEN
                    this.headers.set("Content-Type", "application/json")
                }
                return response.writeWith(Mono.just(
                    response.bufferFactory().wrap(
                        """{"error": "No web session found"}""".toByteArray()
                    )
                ))
            }
        }
        return Mono.error(ex)
    }


}