package io.wispershadow.tech.common.ratelimit.controller

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class TestWebController {

    @RequestMapping(path = ["/byip"], method = [RequestMethod.GET])
    @ResponseBody
    public fun byip(request: ServerHttpRequest): Mono<String> {
        return Mono.just("BY IP: " + request.remoteAddress?.address?.hostAddress)
    }

    @RequestMapping(path = ["/createsession"], method = [RequestMethod.GET])
    @ResponseBody
    public fun createSession(exchange: ServerWebExchange): Mono<String> {
        return exchange.session.map {webSession ->
            webSession.attributes["key1"] = "value1"
            webSession.id
        }
    }
}