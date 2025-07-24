package io.wispershadow.tech.common.http

import javax.net.ssl.SSLException

class HttpClientConfig {
    var connectionTimeoutMillis: Long = 3000
    var readTimeoutMillis: Long = 5000
    var maxTotal: Int = 30
    var maxConnectionPerRoute: Int = 10
    var retriable: Boolean = false
    var maxRetryTimes: Int = 3
    var retryInterval: Long = 1000L
    //var retriableExceptions: List<String> = listOf(SSLException::class.java.name)
}