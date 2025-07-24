package io.wispershadow.tech.common.ratelimit.controller

import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
open class RateLimiterAnnotationWebController {
    @RateLimiter(name = "T(io.wispershadow.tech.common.ratelimit.reactive.RateLimitKeyGenReactive).ip(#request)")
    @RequestMapping(path = ["/byip_anno"], method = [RequestMethod.GET])
    @ResponseBody
    open fun byip(request: ServerHttpRequest): Mono<String> {
        return Mono.just("BY IP: " + request.remoteAddress?.address?.hostAddress)
    }


    @RateLimiter(name = "T(io.wispershadow.tech.common.ratelimit.reactive.RateLimitKeyGenReactive).session(#exchange)")
    @RequestMapping(path = ["/bysession_anno"], method = [RequestMethod.GET])
    @ResponseBody
    open fun bysession(exchange: ServerWebExchange): Mono<String> {
        return Mono.just("ok")
    }

    @RateLimiter(name = "T(io.wispershadow.tech.common.ratelimit.reactive.RateLimitKeyGenReactive).path(#request)")
    @RequestMapping(path = ["/api/path1"], method = [RequestMethod.GET])
    @ResponseBody
    open fun bypath1(request: ServerHttpRequest): Mono<String> {
        return Mono.just(request.path.value())
    }
}