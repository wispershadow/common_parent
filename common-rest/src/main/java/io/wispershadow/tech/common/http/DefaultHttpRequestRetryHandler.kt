package io.wispershadow.tech.common.http

import org.apache.http.client.HttpRequestRetryHandler
import org.apache.http.protocol.HttpContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

class DefaultHttpRequestRetryHandler(val connectionConfig: HttpClientConfig): HttpRequestRetryHandler {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DefaultHttpRequestRetryHandler::class.java)
    }
    override fun retryRequest(ioException: IOException, retryTimes: Int, httpContext: HttpContext): Boolean {
        logger.info("Start retrying http client request with exception: {}, retryTimes: {}",
            ioException::class.java.name, retryTimes)
        if (!connectionConfig.retriable) {
            logger.info("Http client request is not retriable, configuration disabled")
            return false
        }
        if (retryTimes > connectionConfig.maxRetryTimes) {
            logger.info("Http client request is not retriable, maxRetryCount: {}, exceeded",
                connectionConfig.maxRetryTimes)
            return false
        }
        if (connectionConfig.retriableExceptions.isNotEmpty()) {
            connectionConfig.retriableExceptions.forEach { exceptionClassName ->
                try {
                    val exceptionClass = Class.forName(exceptionClassName)
                    if (!exceptionClass.isAssignableFrom(ioException::class.java)) {
                        logger.info("Http client request is not retriable, exception class: {} mismatched", exceptionClassName)
                        return false
                    }
                }
                catch (e: Exception) {
                }
            }
        }
        logger.info("Http client request is retriable")
        return true
    }
}