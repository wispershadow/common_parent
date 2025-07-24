package io.wispershadow.tech.common.ratelimit.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class TestController {
    @RequestMapping(path = ["/byip_standard"], method = [RequestMethod.GET])
    @ResponseBody
    public fun byip(request: HttpServletRequest): ResponseEntity<String> {
        return ResponseEntity.ok("BY IP: " + request.remoteAddr)
    }
}